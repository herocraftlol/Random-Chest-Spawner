# RandomChestSpawner - Plugin Minecraft

Un plugin Spigot/Paper qui fait apparaitre des coffres avec des objets aleatoires de differentes raretes.

## Fonctionnalites

- **Loot par rarete**: 4 tires (Commun, Rare, Epique, Legendaire) avec des chances differentes
- **Anti-abus**: Limite le nombre d'items precieux par coffre
- **Placement au sol**: Place les coffres **sur le sol** sur le bloc solide le plus haut (jamais en lair)
- **Distance intelligente**: Minimum 200 blocs, maximum 2000 blocs du centre des joueurs
- **Limites du monde**: Coordonnees limitees a -10000 et 10000 en X/Z
- **Notifications chat**: Previens tous les joueurs quand un coffre apparait ou est vide
- **Disparition apres pillage**: Le coffre disparait quand il est vide, notification visible par tous
- **Timing**:
  - Intervalle aleatoire entre 20 et 35 minutes
  - Cycle qui fonctionne meme sans joueurs
  - Le coffre spawn uniquement quand des joueurs sont connectes

## Raretes

| Rareté | Chance | Quantité | Exemples |
|--------|--------|----------|----------|
| Commun | 58% | 16-64 | Pierre, bois, bouffe basique |
| Rare | 30% | 4-16 | Fer, or, outils |
| Epique | 9% | 1-4 | Diamant, disque de musique |
| Legendaire | 3% | 1 | Netherite, Elytra, Totem |

## Requirements

- Java 17+
- Serveur Spigot ou Paper 1.21+

## Compilation

```bash
mvn clean package
```

Le JAR compile sera dans `target/RandomChestSpawner-1.0.0.jar`.

## Installation

1. Telechargez `RandomChestSpawner-1.0.0.jar` depuis la page des releases
2. Copiez-le dans le dossier `plugins` de votre serveur
3. Redemarrez votre serveur

## Utilisation

- Le cycle de 20-35 minutes (aleatoire) tourne en permanence (meme sans joueurs)
- Quand un joueur se connecte, il peut avoir de la chance et tomber sur un cycle imminent
- Les coffres contiennent 4-9 items de differentes raretes
- Les coordonnees sont annoncees dans le chat
- Le coffre disparait quand il est vide (pille) avec notification visible par tous
- Le coffre spawn entre 200 et 2000 blocs du centre des joueurs

## Licence

Projet open source sous licence MIT.