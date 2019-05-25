
public class Main {


//java -cp ".:sqlite-jdbc-3.27.2.1.jar" Main
    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm.sqlite");
        //Quand on a pas d'informations sur la transition entre le keyword suivant, on met un ou ?
        System.out.println(uneRecherche.retrouve("avec mccartney"));

        uneRecherche.fermeBase();
        long debut = System.currentTimeMillis();
        System.out.println("Temps d'exécution = " + (System.currentTimeMillis()-debut) + " ms");
    }
   
}