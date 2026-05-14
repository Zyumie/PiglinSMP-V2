<div align="center">

[![Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)
[![Purpur](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)

</div>

---

# 🔥 PiglinSMP — Plugin Minecraft

### 📌 Version 1.0.0

- **Plugin :** PiglinSMP
- **Auteur :** Zyumie (aka Ayano)
- **Compatibilité :** PaperMC / Purpur 1.21+
- **Type :** Custom Piglin Bartering Weapons

---

## 🧠 Concept

**PiglinSMP** ajoute plusieurs armes uniques obtenables uniquement via le **Piglin Bartering**.

Chaque arme possède :
- ses propres capacités
- des effets spéciaux
- des mécaniques PvP/PvE uniques
- des particules et sons personnalisés
- un système de rareté équilibré

Toutes les armes sont reconnues grâce aux **PersistentDataContainer (PDC)** afin d'éviter les faux items ou les duplications.

---

## ⚔️ Fonctionnalités

- **7 armes custom** avec capacités uniques
- **Drop rates personnalisés** via Piglin Bartering
- **Effets spéciaux avancés**
  - poison progressif
  - rage stacking
  - freeze
  - explosions contrôlées
  - levitation
  - invisibilité
  - vols d'items
- **Particles & sounds**
- **Cooldown systems**
- **Configuration simple**
- **Optimisé PvP / SMP**
- **Aucune dépendance**

---

## 🗡️ Armes disponibles

| Arme | Rareté | Drop |
|------|---------|------|
| Arc du Nether Instable | Peu commun | ~1.5% |
| Lame du Poison | Commun | ~1.7% |
| Dague du Voleur | Peu commun | ~1.2% |
| Épée du Temps | Rare | ~0.7% |
| Épée du Berserker | Rare | ~0.8% |
| Épée du Jugement Piglin | Ultra Rare | ~0.1% |
| Bâton du Trickster | Commun | ~1.8% |

---

## 🔥 Capacités des armes

### 🏹 Arc du Nether Instable

- Flèches explosives
- Flèches de feu
- Flèches de lenteur
- Effets aléatoires
- Explosions sans destruction de blocs

---

### ☠️ Lame du Poison

- Poison cumulatif sur les ennemis
- Intensité du poison augmente à chaque hit
- Reset automatique des stacks
- Gameplay lent mais très dangereux

---

### 🗡️ Dague du Voleur

- Peut voler des items ennemis
- Possibilité de drop l'item au sol
- Compatible joueurs et mobs
- Cooldown anti-spam intégré

---

### ⏳ Épée du Temps

- Slowness et Mining Fatigue
- Chance de freeze total
- Particules temporelles
- Contrôle de déplacement

---

### 🔥 Épée du Berserker

- Système de rage stackable
- Bonus de vitesse et dégâts
- Risque de Weakness à haut niveau
- Gameplay agressif

---

### 👑 Épée du Jugement Piglin

- Dégâts augmentent selon l'or de la cible
- Scan de l'inventaire ennemi
- Sons de tonnerre
- Particules gold custom

---

### 🎭 Bâton du Trickster

- Effets totalement aléatoires
- Poison, blindness, levitation, invisibilité...
- Aucun cooldown
- Arme orientée chaos

---

## 🔧 Installation

1. Installer un serveur compatible **PaperMC** ou **Purpur** (1.21+)
2. Télécharger `PiglinWeapons.jar`
3. Placer le fichier dans le dossier `plugins/`
4. Redémarrer le serveur
5. Les armes seront automatiquement ajoutées au système de bartering Piglin

---

## 🎮 Commandes

| Commande | Description | Permission |
|----------|-------------|------------|
| `/pw reload` | Recharge la configuration | `piglinweapons.reload` |
| `/pw give <joueur> <arme>` | Donne une arme custom | `piglinweapons.give` |
| `/pw list` | Liste toutes les armes | `piglinweapons.list` |

---

## 🔑 Permissions

| Permission | Description | Défaut |
|------------|-------------|--------|
| `piglinweapons.reload` | Recharger la config | OP |
| `piglinweapons.give` | Donner une arme | OP |
| `piglinweapons.list` | Voir la liste des armes | true |

---

## ⚙️ Configuration (`config.yml`)

```yaml
# Activation des drops Piglin
Piglin-Bartering:
  enabled: true

# Chances de drop des armes
Drop-Rates:
  unstable-bow: 1.5
  poison-blade: 1.7
  thief-dagger: 1.2
  time-sword: 0.7
  berserker-sword: 0.8
  piglin-judgment-sword: 0.1
  trickster-staff: 1.8

# Arc du Nether
Unstable-Bow:
  explosion-power: 1.2
  explosion-break-blocks: false
  special-arrow-cooldown: 1

# Lame du Poison
Poison-Blade:
  reset-time: 10
  max-poison-level: 3

# Dague du Voleur
Thief-Dagger:
  steal-chance: 35
  cooldown: 2

# Épée du Temps
Time-Sword:
  freeze-chance: 15
  cooldown: 3

# Berserker
Berserker:
  max-rage: 10
  reset-time: 8

# Trickster
Trickster:
  enable-actionbar: true
```

> Le `config.yml` complet est généré automatiquement au premier démarrage.

---

## 📁 Structure du projet

```text
fr.zyumie.PiglinWeapons
├── Main.java
├── Listener/
│   ├── BarterListener
│   ├── CombatListener
│   ├── ProjectileListener
├── Command/
│   ├── PWeaponCommand
├── Manager/
│   ├── CooldownManager
│   └── WeaponManager
└──
```

---

## 🧪 Tests effectués

- Vérification des drop rates
- Tests PvP des effets
- Vérification anti-grief des explosions
- Tests des cooldowns
- Vérification des resets de stacks
- Compatibilité multijoueur

---

## 📊 Requirements

- **Minecraft :** 1.21+
- **Java :** 21+
- **Serveur :** Paper / Purpur
- **Dépendances :** Aucune

---

*© 2026 Zyumie. Tous droits réservés.  
Aucune utilisation, modification ou redistribution sans autorisation explicite.*
