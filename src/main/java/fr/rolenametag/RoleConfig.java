package fr.rolenametag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Config : .minecraft/config/rolenametag.json (modifiable en jeu via le panneau de configuration). */
public final class RoleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("rolenametag.json");
    public static RoleConfig INSTANCE = new RoleConfig();

    /** Interrupteur général. */
    public boolean enabled = true;

    // --- Mon rôle (affiché avant mon pseudo, seulement si le TAB me compte dans ce rôle) ---
    public boolean showOwnRole = true;
    public boolean ownStar = true;      // si je suis Mod / Admin
    public boolean ownCamera = true;    // si je suis Media
    public boolean ownPlus = true;      // croix cyan après la caméra, si je suis Media
    public boolean ownDiamond = true;   // si je suis membre
    /** Afficher mon propre nametag en 3ème personne. */
    public boolean showOnSelf = true;

    // --- Autres joueurs : uniquement le losange bleu des membres ---
    public boolean showMember = true;

    /** Rôle par défaut quand le joueur est dans le TAB sans rôle reconnu. */
    public String defaultRole = "MEMBER";

    /** Logos du serveur (hex) -> rôle. Appris automatiquement. */
    public Map<String, String> glyphRoles = new LinkedHashMap<>();

    public List<String> mediaKeywords = new ArrayList<>(List.of("media", "youtube", "youtuber", "streamer", "tiktok"));
    public List<String> staffKeywords = new ArrayList<>(List.of(
            "owner", "admin", "mod", "moderator", "helper", "dev", "developer", "staff", "manager", "support"));

    /**
     * Logo à placer avant le pseudo, ou null.
     *  - moi : selon mon rôle dans le TAB (étoile / caméra + croix / losange) et les options cochées ;
     *  - les autres : uniquement le losange bleu des membres.
     */
    public MutableText iconFor(Role role, boolean self) {
        if (!enabled) return null;
        StringBuilder sb = new StringBuilder();
        if (self) {
            if (!showOwnRole) return null;
            switch (role) {
                case MEDIA -> { if (ownCamera) sb.append(Role.CAMERA); if (ownPlus) sb.append(Role.PLUS); }
                case STAFF -> { if (ownStar) sb.append(Role.STAR); }
                case MEMBER -> { if (ownDiamond) sb.append(Role.DIAMOND); }
            }
        } else if (role == Role.MEMBER && showMember) {
            sb.append(Role.DIAMOND);
        }
        return sb.isEmpty() ? null : Role.glyph(sb.toString());
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                RoleConfig c = GSON.fromJson(Files.readString(FILE), RoleConfig.class);
                if (c != null) INSTANCE = c;
            }
            save();
        } catch (Exception e) {
            System.err.println("[rolenametag] config illisible, valeurs par défaut : " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            System.err.println("[rolenametag] impossible d'écrire la config : " + e);
        }
    }

    public Role defaultRoleEnum() {
        try { return Role.valueOf(defaultRole.toUpperCase(Locale.ROOT)); }
        catch (Exception e) { return Role.MEMBER; }
    }
}
