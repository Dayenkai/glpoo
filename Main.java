
public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");
        //Quand on a pas d'informations sur la transition entre le keyword suivant, on met un ou ?
        uneRecherche.retrouve("de john wick, alassane dramane ouattara");
        System.out.println(isNumeric("10") + " " + isNumeric("Cinquante50"));
        long debut = System.currentTimeMillis();

        System.out.println("Temps d'exécution = " + (System.currentTimeMillis()-debut) + " ms");
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
      }
   
}