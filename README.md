<h1> Spécifications fonctionnelles </h1>
Le but de ce projet est de réaliser un composant qui pourrait être utilisable par exemple en tant que back-end d'un site web, pour fournir une fonctionnalité de recherche avancée pour retrouver des informations dans une base de films; ce composant pourrait être utilisé sur un site dédié au cinéma (type IMDB) ou un site de streaming.
La recherche dans une base par le grand public est toujours délicate. Il y a essentiellement deux approches:
     => La recherche type Google, dite "plein texte" (full-text) où l'on essaie de faire correspondre les termes cherchés à n'importe quoi dans la base; c'est l'approche par défaut d'IMDB, et c'est ce que l'on trouve le plus souvent. L'inconvénient, c'est que si vous cherchez "Chaplin" par exemple, cela correspond évidemment au réalisateur et acteur bien connu, mais aussi au film tiré de son autobiographie, aux membres de sa famille qui sont intervenus dans le cinéma (son frère Sydney, ses enfants comme un autre Sydney ou Geraldine) ou à Ben Chaplin, un acteur anglais sans relation familiale. Il y a un problème de "bruit", qu'IMDB essaie de résoudre par une liste de sous- catégories dans la barre de recherche, méthode qui a ses limites, et un problème de qualification qui a un côté devinette (dans ce que j'ai trouvé, qu'est-ce qu'il est plus probable que voulait l'utilisateur?)
    => La recherche de type formulaire. Cela peut aller du minimaliste, type Hong Kong Movie Database (hkmdb.com) avec un champ de recherche "Movie" et un champ de recherche "People", à quelque chose de beaucoup plus évolué, comme par exemple le site www.europas-cinemas.org.
 Ce projet explore une troisième voie, où l'on propose à l'utilisateur une syntaxe de recherche extrêmement simple, permettant d'effectuer des recherches relativement précises sans aucune connaissance de SQL. Le langage d'interrogation est spécifique au thème de la base (le cinéma) et a très peu de mots-clefs; ceux-ci pourront être entrés en majuscules, en minuscules, ou N'imPorTe CoMment. Le composant prendra cette requête simple en entrée, la transformera en requête SQL, exécutera la requête, et retournera le résultat comme une chaîne de caractère au format JSON, facile à exploiter dans un langage tel que Javascript ou Python.

Le langage de recherche simplifiée
Celui-ci s'appuie sur très peu de mots-clefs: TITRE suivi d'un titre de film
DE suivi d'un nom de réalisateur
AVEC suivi d'un nom d'acteur ou d'actrice
PAYS suivi d'un code (ISO sur deux lettres) ou nom de pays
EN suivi d'une année de sortie
AVANT suivi d'une année de sortie (correspond à <, on ne traite pas <=)
APRES (ou APRÈS) suivi d'une année de sortie (correspond à >, on ne traite pas >=)