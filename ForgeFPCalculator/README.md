# Forge FP Calculator

Application Android **Kotlin + Jetpack Compose** pour préparer des niveaux de Grands Bâtiments de *Forge of Empires* avec un fil 1,9.

## Fonctionnalités

L’application intègre une base locale de **49 Grands Bâtiments** et **12 830 niveaux** extraite du classeur fourni *Linnun's FoE Efficiency*. Le sélecteur pré-remplit le coût du niveau et les cinq récompenses de contribution. Une saisie manuelle reste disponible pour tout GB ou niveau non présent dans le catalogue.

Pour chaque emplacement, le montant à publier est calculé avec `ceil(récompense × 1,9)`. Le seuil propriétaire anti-snipe est ensuite :

```text
coût du niveau − somme des contributions des places supérieures − 2 × contribution de la place
```

Le calcul laisse donc au plus deux fois le montant de l’emplacement dans le GB une fois le seuil atteint. L’écran affiche les cinq emplacements, le montant à publier, le seuil propriétaire, l’ajout nécessaire depuis l’état actuel et l’ordre recommandé P1 → P5.

## Interface

L'écran conserve le parcours original (catalogue ou saisie manuelle, paramètres du fil, résultats, copie/partage et documents), avec une présentation repensée : bandeau illustré par le GB sélectionné, vue rapide des places sur téléphone, tableau détaillé sur tablette et feuille de route P1 → P5. Le contenu reste utilisable avec les données locales, sans connexion réseau.

## Lecture des classeurs Excel

Pour les fichiers **`.xlsx`**, le lecteur conserve les numéros de lignes Excel, y compris les lignes absentes ou cachées, afin que les images et les couleurs restent ancrées aux bonnes cellules. Il restitue les remplissages directs, les couleurs de thème (avec teinte), les palettes indexées, les styles de lignes/colonnes et les remplissages différentiels. Les règles conditionnelles prises en charge sont « contient/ne contient pas », « commence/termine par », les comparaisons numériques, les dégradés à 2 ou 3 couleurs ainsi que certaines expressions `SEARCH`/`FIND`/`COUNTIF` simples. Les formules Excel arbitraires, les barres de données et les jeux d’icônes ne sont pas évalués : une règle inconnue est ignorée plutôt que de colorer faussement la feuille.

La lecture des **`.xls`** via JXL reste limitée aux valeurs des cellules ; la restitution des images et des mises en forme décrite ci-dessus concerne `.xlsx`.

## Construire l’APK

Pré-requis : JDK 17+ et Android SDK avec la plateforme 35. Le Gradle Wrapper est inclus.

```bash
./gradlew test
./gradlew assembleDebug
```

L’APK de débogage est généré à :

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Données intégrées

`app/src/main/assets/great_buildings.json` est généré depuis le classeur Excel fourni. Pour refaire l’export dans cet environnement :

```bash
python3 /home/ubuntu/export_building_data.py
```

Les niveaux dont le classeur ne comporte pas de récompenses de contribution ne sont volontairement pas importés : l’app ne fabrique aucune donnée.

## Tests

Les tests unitaires vérifient l’arrondi supérieur du montant 1,9, la formule anti-snipe, l’absence d’ajout lorsque le seuil est déjà atteint, et la progression séquentielle des places.

Huit tests du lecteur `.xlsx` couvrent également les coordonnées physiques des lignes vides/cachées et des images, les fusions, les couleurs indexées/de thème et le repli sur `bgColor`, les styles de lignes/colonnes, ainsi que les règles conditionnelles (texte contenu, formules courantes, priorités et dégradés). La CI exécute `testDebugUnitTest assembleDebug` avec JDK 17 et Android SDK 35. Pour les anciens fichiers `.xls`, la lecture des valeurs est conservée ; les couleurs et règles conditionnelles détaillées concernent le format `.xlsx`.
