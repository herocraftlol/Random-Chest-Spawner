# RandomChestSpawner - Plugin Minecraft

Un plugin Spigot/Paper qui fait apparaitre des coffres avec des objets aleatoires a des emplacements varies pour les joueurs en ligne.

## Fonctionnalites

- **Apparition aleatoire de coffres**: Fait apparaitre un coffre avec 4 a 9 objets aleatoires (survie uniquement, sans spawn eggs)
- **Distance centree**: Apparait au centre de tous les joueurs connectes, minimum 200 blocs
- **Distance intelligente**: Maximum 2000 blocs (ou plus si les joueurs sont tres eloignes)
- **Placement au sol**: Place les coffres **sur le sol** sur le bloc solide le plus haut (jamais en lair)
- **Limites du monde**: Coordonnees limitees a -10000 et 10000 en X/Z
- **Notifications chat**: Previens tous les joueurs quand un coffre apparait ou est vide
- **Disparition apres pillage**: Le coffre disparait quand il est vide, notification visible par tous
- **Timing**:
  - Intervalle aleatoire entre 20 et 35 minutes
  - Cycle qui fonctionne meme sans joueurs
  - Le coffre spawn uniquement quand des joueurs sont connectes

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
- Les coffres contiennent 4 a 9 objets aleatoires (survie uniquement, sans spawn eggs)
- Les coordonnees sont annoncees dans le chat
- Le coffre disparait quand il est vide (pille) avec notification visible par tous
- Le coffre spawn entre 200 et 2000 blocs du centre des joueurs

## Licence

Projet open source sous licence MIT.
