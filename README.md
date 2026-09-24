# Forge FP Calculator — interface revisitée

Le projet Android Studio se trouve dans [`ForgeFPCalculator/`](ForgeFPCalculator/). L’archive [`ForgeFPCalculator-AndroidStudio-updated.zip`](ForgeFPCalculator-AndroidStudio-updated.zip) contient également le projet mis à jour pour ceux qui préfèrent l’importer directement.

## Ce qui a changé

- Nouvelle identité visuelle ardoise, turquoise et or, avec l’illustration **du bâtiment réellement sélectionné**.
- Parcours plus clair : préparation du niveau → résultat → emplacements → ordre de pose.
- Affichage adapté au téléphone (vue rapide des cinq places, cartes lisibles) et à la tablette (tableau complet, panneaux en colonnes).
- Sélecteurs de bâtiment/niveau, bibliothèque de documents et états vides harmonisés.
- Formule explicative synchronisée avec le multiplicateur choisi ; boutons de copie et de partage conservés.
- Lecture `.xlsx` fiabilisée : lignes vides/cachées alignées avec les images, couleurs de thème/indexées, fonds `fgColor`/`bgColor` et principales mises en forme conditionnelles (dont dégradés et texte contenu).

La structure Android, le catalogue local, le lecteur PDF et la formule de calcul sont conservés. Des tests de non-régression ciblent désormais le lecteur Excel.

## Ouvrir et vérifier

Avec **JDK 17+** et **Android SDK 35**, ouvrez `ForgeFPCalculator/` dans Android Studio, ou exécutez :

```bash
cd ForgeFPCalculator
./gradlew test
./gradlew assembleDebug
```

La clé de signature de l’archive d’origine n’est pas dupliquée dans les sources suivies séparément. Pour une publication depuis le dossier source, fournissez votre propre clé de signature. Le débogage et les tests n’en ont pas besoin.
