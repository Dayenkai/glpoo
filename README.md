<h1> Functional specifications </h1>
 The purpose of this project is to produce a component that could be usable for example as a back-end of a website, to provide an advanced search functionality to retrieve information in a movie database; this component could be used on a site dedicated to cinema (type IMDB) or a streaming site.<br />
Research on a base by the general public is always delicate. There are essentially two approaches: <br />
=> The Google search, called "full text" (full-text) where we try to match the search terms to anything in the database; this is the default approach of IMDB, and this is what we find most often. The downside is that if you’re looking for "Chaplin," for example, that obviously corresponds to the well-known director and actor, but also to the film taken from his autobiography, to the members of his family who intervened in the cinema (his brother Sydney, his children like another Sydney or Geraldine) or to Ben Chaplin, an English actor without a family relationship. There is a problem of "noise", which IMDB tries to solve by a list of subcategories in the search bar, method that has its limits, and a qualification problem that has a riddle side (in what I found, what is more likely than the user wanted?) <br />
=> Form type search. This can range from minimalist, such as Hong Kong Movie Database (hkmdb.com) with a "Movie" search field and a "People" search field, to something much more advanced, such as www.europas-cinemas.org. <br />
This project explores a third path, where the user is offered an extremely simple search syntax, allowing to carry out relatively precise searches without any knowledge of SQL. The query language is specific to the theme of the base (cinema) and has very few keywords; these can be entered in upper case, lower case, or N'imPorTe Comment. The component will take this simple input query, turn it into an SQL query, execute the query, and return the result as a string in JSON format, easy to use in a language such as Javascript or Python.<br />

<h2> Simplified search language </h2>
It is based on very few keywords: TITLE followed by a movie title <br />
FOLLOWED BY ACTOR OR ACTRESS NAME <br />
COUNTRY followed by a code (ISO on two letters) or country name <br />
EN followed by a release year <br />
BEFORE followed by an exit year (corresponds to , we do not process =) <br />
APRES (or APRÈS) followed by a release year (corresponds to >, we do not process >=)<br />
