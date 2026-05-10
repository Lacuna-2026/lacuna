# 🤖 Documentation IA — Minimax pour Lacuna

## Vue d'ensemble

L'IA utilise l'algorithme **Minimax avec élagage Alpha-Beta** pour jouer au jeu Lacuna.  
Elle simule la fin de partie complète (phase de résolution incluse) pour choisir le meilleur coup.

---

## Architecture

```
lacuna/model/AI/
├── AIFlower.java    — Snapshot d'une fleur
├── AIPawn.java      — Snapshot d'un pion
├── AIMove.java      — Représentation d'un coup
├── AIState.java     — État complet du jeu (un nœud de l'arbre)
├── Minimax.java     — Algorithme Minimax + évaluation
└── TestMinimax.java — Test console IA vs IA
```

---

## Structures de données

### `AIFlower`
Copie légère d'une fleur du jeu.

| Champ | Type | Description |
|-------|------|-------------|
| `index` | `int` | Index dans la liste des fleurs du GameModel |
| `color` | `FlowerColor` | Couleur de la fleur |
| `x`, `y` | `double` | Position sur le tapis (-1.0 à 1.0) |
| `ownerIndex` | `int` | `-1` = sur le tapis, `0` = joueur 0, `1` = joueur 1 |

**Méthodes** : `copy()`, `isOnBoard()`

---

### `AIPawn`
Copie légère d'un pion.

| Champ | Type | Description |
|-------|------|-------------|
| `ownerIndex` | `int` | Joueur propriétaire (0 ou 1) |
| `x`, `y` | `double` | Position (non significatif si `placed == false`) |
| `placed` | `boolean` | `true` si le pion est posé sur le tapis |

**Méthodes** : `copy()`

---

### `AIMove`
Représente un coup possible.

| Champ | Type | Description |
|-------|------|-------------|
| `flower1Index` | `int` | Index de la première fleur à relier |
| `flower2Index` | `int` | Index de la seconde fleur à relier |
| `pawnX`, `pawnY` | `double` | Position optimale où poser le pion |

---

### `AIState`
Snapshot complet du jeu = un nœud de l'arbre Minimax.

| Champ | Type | Description |
|-------|------|-------------|
| `flowers` | `AIFlower[]` | 49 fleurs (7 couleurs × 7) |
| `pawns` | `AIPawn[]` | 12 pions (`[0..5]` = joueur 0, `[6..11]` = joueur 1) |
| `currentPlayer` | `int` | Joueur dont c'est le tour (0 ou 1) |
| `pawnsPlaced` | `int[]` | Nombre de pions posés par joueur |

**Méthodes clés** :
- `fromModel(GameModel)` — Crée un snapshot depuis le modèle (appelé une seule fois à la racine)
- `copy()` — Copie profonde pour brancher dans l'arbre
- `isTerminal()` — Vérifie si les deux joueurs ont placé tous leurs pions

---

## Algorithme Minimax

### Fonctionnement

```
findBestMove()
│
├── Génère tous les coups valides (generateMoves)
├── Trie les coups par heuristique rapide (move ordering)
│
└── Pour chaque coup :
    ├── Applique le coup → nouvel état (applyMove)
    └── minimax(état, profondeur, α, β, maximizing)
        │
        ├── Si profondeur == 0 ou terminal → evaluate(état)
        │
        ├── Si maximizing (tour de l'IA) :
        │   └── Cherche le score MAX parmi les enfants
        │       Élagage : if β ≤ α → BREAK (coupe la branche)
        │
        └── Si minimizing (tour de l'adversaire) :
            └── Cherche le score MIN parmi les enfants
                Élagage : if β ≤ α → BREAK (coupe la branche)
```

### Élagage Alpha-Beta

L'élagage Alpha-Beta évite d'explorer des branches inutiles :
- **α (alpha)** = meilleur score garanti pour le maximiseur
- **β (beta)** = meilleur score garanti pour le minimiseur
- Si `β ≤ α`, on arrête d'explorer cette branche

Cela réduit le nombre de nœuds explorés de O(b^d) à O(b^(d/2)) dans le meilleur cas.

---

## Optimisations

| Optimisation | Description |
|-------------|-------------|
| **Pré-groupement par couleur** | Les fleurs sont groupées par couleur au départ, évitant de tester toutes les paires O(n²) |
| **Move ordering** | Les coups sont triés par heuristique rapide à la racine pour maximiser l'élagage |
| **Profondeur adaptative** | Plus profond quand il y a peu de coups (≤5 coups → +2 profondeur) |
| **Limite de temps** | La recherche s'arrête si le temps imparti est dépassé |
| **dist² au lieu de sqrt** | Toutes les comparaisons de distance utilisent le carré (plus rapide) |
| **1 position par paire** | Seule la meilleure position de pion est gardée par paire de fleurs |

---

## Placement intelligent du pion

Pour chaque paire de fleurs valide, l'IA teste **5 positions** le long du segment :
- `t = 0.15, 0.30, 0.50, 0.70, 0.85` (paramètre entre fleur 1 et fleur 2)

Elle choisit la position qui **maximise le nombre de fleurs restantes** dont ce pion serait le plus proche (= les fleurs qu'il gagnerait en phase de résolution).

Un poids stratégique est appliqué par couleur :
- **×3** pour les couleurs où l'IA est en retard
- **×2** pour les couleurs contestées
- **×2.5** pour les couleurs verrouillables (3 fleurs sur 7)

---

## Fonction d'évaluation

L'évaluation **simule la résolution complète** :

1. **Fleurs déjà capturées** → comptées directement
2. **Fleurs restantes sur le tapis** → attribuées au pion le plus proche (simulation)
3. **Majorités par couleur** → calculées sur le total (capturées + simulées)

### Scores

| Situation | Score |
|-----------|-------|
| Victoire (≥ 4 majorités) | +50 000 |
| Défaite | -50 000 |
| Majorité gagnée | +200 + marge × 30 |
| Majorité perdue | -200 - marge × 30 |
| 3 majorités (presque gagné) | +1 000 |
| Avantage par fleur | +10 par fleur d'avance |

---

## Niveaux de difficulté

| Niveau | Profondeur | Temps max | Comportement |
|--------|-----------|-----------|--------------|
| Facile | 2 | 0.5s | Réaction instantanée, jeu basique |
| Moyen | 3 | 1.5s | Jeu solide, anticipe 3 coups |
| Difficile | 4 | 3s | Jeu fort, simule la résolution |

---

## Utilisation

```java
// Créer l'IA pour le joueur 1, profondeur 4, limite 3 secondes
Minimax ai = new Minimax(model, 1, 4, 3000);

// Trouver le meilleur coup
AIMove best = ai.findBestMove();

// Appliquer sur le modèle
Flower f1 = model.getFleurs().get(best.flower1Index);
Flower f2 = model.getFleurs().get(best.flower2Index);
model.placerPionEtCapturer(f1, f2, best.pawnX, best.pawnY);
```

---

## Test

Lancer le test IA vs IA en console :

```bash
javac -d out -sourcepath src src/lacuna/model/AI/TestMinimax.java
java -cp out lacuna.model.AI.TestMinimax
```

Le test joue une partie complète avec les deux joueurs contrôlés par Minimax et affiche chaque coup, le temps de calcul, et le résultat final avec les majorités par couleur.
