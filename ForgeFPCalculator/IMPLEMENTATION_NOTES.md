# Notes d’implémentation

## Données

Le catalogue `great_buildings.json` a été créé à partir du classeur **Linnun's FoE Efficiency** fourni dans la demande. Les 49 feuilles de Grands Bâtiments ont été lues et les colonnes de contribution P1 à P5 ont été exportées avec le coût du niveau. Le fichier embarqué contient 12 830 enregistrements de niveau dont les cinq récompenses de contribution sont effectivement renseignées par le classeur. Aucune valeur manquante n’a été estimée ou prolongée.

## Règle de calcul

Pour un emplacement `n`, l’application calcule d’abord le montant demandé au contributeur :

```text
contribution(n) = ceil(récompense(n) × multiplicateur)
```

Le multiplicateur est 1,9 par défaut. Le seuil auquel le propriétaire doit être arrivé pour que l’emplacement soit couvert est :

```text
seuil propriétaire(n) = coût du niveau
                        − somme(contributions P1 à P(n−1))
                        − 2 × contribution(n)
```

Le seuil est borné à zéro. La quantité à ajouter dans l’état affiché correspond à `max(0, seuil − FP propriétaire déjà placés)`. La séquence P1 → P5 met à jour l’avance du propriétaire après chaque ajout requis.

## Interprétation de la protection

Une fois le seuil atteint, et après comptabilisation des places mieux classées au montant affiché, le reste disponible pour l’emplacement ne dépasse pas deux fois sa contribution. C’est la contrainte de sécurité anti-snipe utilisée par les calculateurs 1,9 : un autre joueur ne peut plus prendre la place et la verrouiller avec une marge de profit.

Les montants sont calculés en FP entiers et la contribution du fil est toujours arrondie au supérieur. Les tests unitaires couvrent ce comportement, les seuils anti-snipe de référence et la progression séquentielle.
