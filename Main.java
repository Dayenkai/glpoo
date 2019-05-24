
public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm.sqlite");
        //Quand on a pas d'informations sur la transition entre le keyword suivant, on met un ou ?
        System.out.println(uneRecherche.retrouve("en 1998"));
       
        uneRecherche.fermeBase();
        long debut = System.currentTimeMillis();
        System.out.println("Temps d'exécution = " + (System.currentTimeMillis()-debut) + " ms");
    }
   
}