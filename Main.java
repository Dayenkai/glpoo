

public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");

        uneRecherche.retrouve("de hitchcock, avec lamar");

        uneRecherche.fermeBase();
    }
}