

public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");

        uneRecherche.retrouve("en 1997 ");

        uneRecherche.fermeBase();
    }
}