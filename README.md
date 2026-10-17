# RankTag 3.1.0 (Fabric 1.21.11, client)

Aucune configuration : le mod lit le TAB de tous les joueurs et attribue tout seul le logo
- MEDIA  -> caméra rose + croix cyan
- MEMBRE -> losange bleu
- MOD/ADMIN -> étoile verte
devant le pseudo, y compris le tien en 3ème personne. Les logos du serveur sont retirés du nametag (anti-superposition).

Détection : logos du serveur dans le TAB (le plus répandu = membre, 2 logos = media, autre logo = staff),
mots-clés dans le préfixe/nom de team (media, admin, mod...).

Commandes (facultatives) : /rolenametag toggle | dump | map <hex> <rôle>

## Obtenir le .jar
Option A (sans rien installer) : crée un dépôt GitHub, envoie ce dossier, onglet Actions -> "build" -> télécharge l'artefact rolenametag-jar.
Option B (local) : JDK 21 + Gradle 9, puis `gradle build` ; le jar est dans build/libs/rolenametag-3.1.0.jar.
Mets-le dans .minecraft/mods avec Fabric Loader 0.18+ et Fabric API (1.21.11).

## Panneau de configuration
Touche par défaut : Maj droite (modifiable dans Options > Contrôles > Role Nametag).
Catégories : Autres joueurs, Mon rôle, Logos, Avancé.
