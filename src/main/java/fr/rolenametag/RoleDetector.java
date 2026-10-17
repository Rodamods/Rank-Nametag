package fr.rolenametag;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Détection automatique adaptée à DonutSMP :
 *  - un joueur ABSENT du TAB (ou sans logo) = MEMBRE (losange bleu) ;
 *  - dans le TAB, il n'y a que des media (caméra) et du staff (étoile) ;
 *  - le mod apprend tout seul quel logo est lequel : le logo le plus répandu dans le TAB = MEDIA,
 *    les autres logos (rares) = STAFF. Ce qu'il apprend est mémorisé dans la config.
 */
public final class RoleDetector {
    private RoleDetector() {}

    private static final Map<UUID, Role> CACHE = new HashMap<>();
    private static long lastRefresh = 0;

    /**
     * Un "logo" = caractère privé OU symbole Unicode (emoji caméra, étoile, losange...).
     * Les lettres, chiffres, espaces et ponctuation ne comptent pas.
     */
    public static boolean isGlyph(int cp) {
        if (cp < 0x80) return false;
        int t = Character.getType(cp);
        return t == Character.PRIVATE_USE || t == Character.OTHER_SYMBOL || t == Character.SURROGATE;
    }

    /** Symboles connus d'office (en plus de l'apprentissage automatique). */
    private static final Map<Integer, Role> DEFAULTS = Map.ofEntries(
            // caméras -> MEDIA
            Map.entry(0x1F4F9, Role.MEDIA), Map.entry(0x1F4F7, Role.MEDIA), Map.entry(0x1F3A5, Role.MEDIA),
            Map.entry(0x1F3AC, Role.MEDIA), Map.entry(0x1F4F8, Role.MEDIA), Map.entry(0x1F4FD, Role.MEDIA),
            // étoiles -> STAFF
            Map.entry(0x2605, Role.STAFF), Map.entry(0x2606, Role.STAFF), Map.entry(0x2B50, Role.STAFF),
            Map.entry(0x1F31F, Role.STAFF), Map.entry(0x272A, Role.STAFF), Map.entry(0x2729, Role.STAFF));

    public static void clearCache() { CACHE.clear(); lastRefresh = 0; }

    public static Text tabText(PlayerListEntry entry, String playerName) {
        Text display = entry.getDisplayName();
        if (display != null) return display;
        return Team.decorateName(entry.getScoreboardTeam(), Text.literal(playerName));
    }

    /** Tous les textes du TAB qui concernent ce joueur. */
    private static List<String> sources(PlayerListEntry entry, String name) {
        List<String> out = new ArrayList<>();
        out.add(tabText(entry, name == null ? "?" : name).getString());
        Team team = entry.getScoreboardTeam();
        if (team != null) {
            out.add(team.getPrefix().getString());
            out.add(team.getSuffix().getString());
            out.add(team.getName());
        }
        return out;
    }

    private static Set<Integer> glyphsOf(PlayerListEntry entry) {
        Set<Integer> g = new LinkedHashSet<>();
        for (String s : sources(entry, null)) s.codePoints().filter(RoleDetector::isGlyph).forEach(g::add);
        return g;
    }

    /** 1 fois par seconde : apprend quel logo = media (le plus répandu) et lesquels = staff. */
    private static void refresh(ClientPlayNetworkHandler net) {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < 1000) return;
        lastRefresh = now;
        CACHE.clear();

        Map<Integer, Integer> counts = new HashMap<>();
        int total = 0;
        for (PlayerListEntry e : net.getPlayerList()) {
            Set<Integer> g = glyphsOf(e);
            if (g.size() == 1) {
                counts.merge(g.iterator().next(), 1, Integer::sum);
                total++;
            }
        }
        if (total < 5) return;

        int top = -1, topCount = 0, secondCount = 0;
        for (Map.Entry<Integer, Integer> en : counts.entrySet()) {
            if (en.getValue() > topCount) { secondCount = topCount; top = en.getKey(); topCount = en.getValue(); }
            else if (en.getValue() > secondCount) secondCount = en.getValue();
        }
        // on n'apprend que si c'est net (évite de mémoriser une erreur)
        if (topCount < 3 || topCount < 2 * secondCount) return;

        RoleConfig cfg = RoleConfig.INSTANCE;
        boolean changed = false;
        for (int cp : counts.keySet()) {
            String key = String.format("%04X", cp);
            if (!cfg.glyphRoles.containsKey(key)) {
                cfg.glyphRoles.put(key, (cp == top ? Role.MEDIA : Role.STAFF).name());
                changed = true;
            }
        }
        if (changed) RoleConfig.save();
    }

    public static Role detect(UUID uuid, String playerName) {
        ClientPlayNetworkHandler net = MinecraftClient.getInstance().getNetworkHandler();
        if (net == null) return null;
        refresh(net);

        Role cached = CACHE.get(uuid);
        if (cached != null) return cached;

        PlayerListEntry entry = net.getPlayerListEntry(uuid);
        if (entry == null) {                 // pas dans le TAB = membre
            CACHE.put(uuid, Role.MEMBER);
            return Role.MEMBER;
        }

        RoleConfig cfg = RoleConfig.INSTANCE;
        Role best = null;
        Set<Integer> glyphs = glyphsOf(entry);

        // 1) logos déjà connus (appris automatiquement ou forcés dans la config)
        for (int cp : glyphs) {
            String mapped = cfg.glyphRoles.get(String.format("%04X", cp));
            if (mapped != null) best = better(best, parse(mapped));
            else if (DEFAULTS.containsKey(cp)) best = better(best, DEFAULTS.get(cp));
        }

        // 2) mots-clés dans le TAB / la team (le pseudo est retiré pour éviter les faux positifs)
        StringBuilder plain = new StringBuilder();
        for (String s : sources(entry, playerName)) {
            s.codePoints().filter(cp -> !isGlyph(cp)).forEach(plain::appendCodePoint);
            plain.append(' ');
        }
        String text = plain.toString().toLowerCase(Locale.ROOT).replace(playerName.toLowerCase(Locale.ROOT), " ");
        for (String token : text.split("[^a-zéèàù]+")) {
            if (token.isEmpty()) continue;
            if (cfg.staffKeywords.contains(token)) best = better(best, Role.STAFF);
            else if (cfg.mediaKeywords.contains(token)) best = better(best, Role.MEDIA);
        }

        // 3) logo pas encore appris : dans le TAB, ça ne peut être qu'un media (ou 2 logos = caméra + croix)
        if (best == null && !glyphs.isEmpty()) best = Role.MEDIA;

        Role result = best != null ? best : cfg.defaultRoleEnum(); // aucun logo = membre
        CACHE.put(uuid, result);
        return result;
    }

    private static Role parse(String s) {
        try { return Role.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Exception e) { return null; }
    }

    private static Role better(Role a, Role b) {
        if (b == null) return a;
        if (a == null) return b;
        return b.priority > a.priority ? b : a;
    }

    /** Retire les logos du serveur du nametag (anti-superposition) et les espaces laissés en tête. */
    public static Text stripServerGlyphs(Text original) {
        MutableText out = Text.empty();
        boolean[] started = {false};
        original.visit((Style style, String s) -> {
            StringBuilder sb = new StringBuilder();
            s.codePoints().forEach(cp -> {
                if (isGlyph(cp)) return;
                if (!started[0] && Character.isWhitespace(cp)) return;
                started[0] = true;
                sb.appendCodePoint(cp);
            });
            if (!sb.isEmpty()) out.append(Text.literal(sb.toString()).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
