
public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");
        //Quand on a pas d'informations sur la transition entre le keyword suivant, on met un ou ?
        uneRecherche.retrouve("de john wick, alassane dramane ouattara");

        long debut = System.currentTimeMillis();

        System.out.println("Temps d'exécution = " + (System.currentTimeMillis()-debut) + " ms");
    }
   
}