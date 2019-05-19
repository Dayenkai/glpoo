

public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");
        //Quand on a pas d'informations sur la transition entre le keyword suivant, on met un ou ?
        uneRecherche.retrouve("apres 1995, apres 1998");

        uneRecherche.fermeBase();

        long debut = System.currentTimeMillis();

        System.out.println(System.currentTimeMillis()-debut + " ms");
    }
}