package fr.rolenametag.gui;

import fr.rolenametag.Role;
import fr.rolenametag.RoleConfig;
import fr.rolenametag.RoleNametagClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** RankTag : en-tête (logo + titre), catégories à gauche, cartes ON/OFF à droite. */
public class RoleConfigScreen extends Screen {
    private static final int C_BG = 0xCC08080E, C_PANEL = 0xFF16161E, C_CARD = 0xFF1C1C26,
            C_BORDER = 0xFF34344A, C_TITLE = 0xFFA06CFF, C_TEXT = 0xFFE8E8F2, C_DESC = 0xFFB4B4C6;
    private static final int HDR = 34, M = 4, LEFT_W = 124, GAP = 6;
    private static final String[] CATS = {"Autres joueurs", "Mon rôle", "On / Off"};
    private static final String TITLE = "RankTag for Donutsmp By Rodadounuts";

    private record Card(String title, String desc, Text preview, BooleanSupplier get, Consumer<Boolean> set) {}

    private final Screen parent;
    private int category = 0;
    private final List<Card> cards = new ArrayList<>();
    private final List<ButtonWidget> buttons = new ArrayList<>();
    private int px, py, pw, ph, contentY, contentH, areaX, areaY, areaW, cardW, cardH, cols;

    public RoleConfigScreen(Screen parent) {
        super(Text.literal("RankTag"));
        this.parent = parent;
    }

    /** Cadre de 1 px dessiné avec fill (drawBorder n'existe plus en 1.21.11). */
    private static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y, x + 1, y + h, color);
        c.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static Text label(boolean on) {
        return Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED);
    }

    private void buildCards() {
        RoleConfig c = RoleConfig.INSTANCE;
        cards.clear();
        switch (category) {
            case 0 -> cards.add(new Card("Losange bleu (Membres)",
                    "Affiche le losange bleu sur les joueurs sans rôle.",
                    Role.glyph(Role.DIAMOND), () -> c.showMember, v -> c.showMember = v));
            case 1 -> {
                cards.add(new Card("Mon rôle", "Affiche mon logo avant mon pseudo.",
                        null, () -> c.showOwnRole, v -> c.showOwnRole = v));
                cards.add(new Card("Étoile (Mod / Admin)", "Si tu es Mod ou Admin dans le TAB.",
                        Role.glyph(Role.STAR), () -> c.ownStar, v -> c.ownStar = v));
                cards.add(new Card("Caméra (Media)", "Si tu es Media dans le TAB.",
                        Role.glyph(Role.CAMERA), () -> c.ownCamera, v -> c.ownCamera = v));
                cards.add(new Card("Croix + (Media)", "Après la caméra, si tu es Media.",
                        Role.glyph(Role.PLUS), () -> c.ownPlus, v -> c.ownPlus = v));
                cards.add(new Card("Losange (Membre)", "Si tu es membre, sans rôle dans le TAB.",
                        Role.glyph(Role.DIAMOND), () -> c.ownDiamond, v -> c.ownDiamond = v));
                cards.add(new Card("Pseudo en 3ème personne", "Affiche mon nametag en F5.",
                        null, () -> c.showOnSelf, v -> c.showOnSelf = v));
            }
            default -> cards.add(new Card("Mod activé", "Active ou désactive tous les logos du mod.",
                    null, () -> c.enabled, v -> c.enabled = v));
        }
    }

    @Override
    protected void init() {
        buttons.clear();
        pw = Math.min(width - 16, 540);
        ph = Math.min(height - 16, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        contentY = py + HDR + M;
        contentH = ph - HDR - M;
        buildCards();

        // catégories (boîte de gauche)
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            Text t = i == category
                    ? Text.literal(CATS[i]).formatted(Formatting.AQUA, Formatting.UNDERLINE)
                    : Text.literal(CATS[i]).formatted(Formatting.WHITE);
            buttons.add(addDrawableChild(ButtonWidget.builder(t, b -> {
                category = idx;
                clearChildren();
                init();
            }).dimensions(px + 8, contentY + 28 + i * 24, LEFT_W - 16, 20).build()));
        }
        buttons.add(addDrawableChild(ButtonWidget.builder(Text.literal("Terminé"), b -> close())
                .dimensions(px + 8, contentY + contentH - 28, LEFT_W - 16, 20).build()));

        // cartes (boîte de droite)
        int rightX = px + LEFT_W + M;
        int rightW = pw - LEFT_W - M;
        areaX = rightX + 8;
        areaY = contentY + 8;
        areaW = rightW - 16;
        int areaH = contentH - 16;
        int n = Math.max(1, cards.size());
        cols = n > 4 ? 2 : 1;
        int rows = (n + cols - 1) / cols;
        cardW = (areaW - GAP * (cols - 1)) / cols;
        cardH = Math.max(36, Math.min(58, (areaH - GAP * (rows - 1)) / rows));
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int cx = cardX(i), cy = cardY(i);
            buttons.add(addDrawableChild(ButtonWidget.builder(label(card.get().getAsBoolean()), b -> {
                boolean v = !card.get().getAsBoolean();
                card.set().accept(v);
                RoleConfig.save();
                b.setMessage(label(v));
                RoleNametagClient.refreshTags();
            }).dimensions(cx + 10, cy + 19, 56, 16).build()));
        }
    }

    private int cardX(int i) { return areaX + (i % cols) * (cardW + GAP); }
    private int cardY(int i) { return areaY + (i / cols) * (cardH + GAP); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, C_BG);

        // en-tête : logo à gauche + titre centré
        context.fill(px, py, px + pw, py + HDR, C_PANEL);
        border(context, px, py, pw, HDR, C_BORDER);
        // glyphe du logo : ascent 22 / baseline à y+7 -> le haut du logo est à y-15
        context.drawText(textRenderer, Role.glyph(Role.LOGO), px + 6, py + 5 + 15, 0xFFFFFFFF, false);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(TITLE), px + pw / 2, py + (HDR - 8) / 2, C_TEXT);

        // boîte de gauche (catégories) et boîte de droite (cartes)
        context.fill(px, contentY, px + LEFT_W, contentY + contentH, C_PANEL);
        border(context, px, contentY, LEFT_W, contentH, C_BORDER);
        int rightX = px + LEFT_W + M;
        int rightW = pw - LEFT_W - M;
        context.fill(rightX, contentY, rightX + rightW, contentY + contentH, C_PANEL);
        border(context, rightX, contentY, rightW, contentH, C_BORDER);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Catégories"), px + LEFT_W / 2, contentY + 10, C_TITLE);

        // fonds des cartes
        for (int i = 0; i < cards.size(); i++) {
            int cx = cardX(i), cy = cardY(i);
            context.fill(cx, cy, cx + cardW, cy + cardH, C_CARD);
            border(context, cx, cy, cardW, cardH, C_BORDER);
            context.fill(cx + 10, cy + 15, cx + cardW - 10, cy + 16, C_BORDER);
        }

        // boutons (rendus à la main)
        for (ButtonWidget b : buttons) b.render(context, mouseX, mouseY, delta);

        // textes + aperçus des logos
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            int cx = cardX(i), cy = cardY(i);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(card.title()), cx + cardW / 2, cy + 4, C_TEXT);
            if (card.preview() != null) {
                context.drawText(textRenderer, card.preview(), cx + cardW - 34, cy + 4, 0xFFFFFFFF, false);
            }
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(card.desc()), cardW - 86);
            for (int l = 0; l < Math.min(lines.size(), 3); l++) {
                context.drawText(textRenderer, lines.get(l), cx + 76, cy + 20 + l * 10, C_DESC, false);
            }
        }
    }

    @Override
    public void close() {
        RoleConfig.save();
        if (client != null) client.setScreen(parent);
    }
}
