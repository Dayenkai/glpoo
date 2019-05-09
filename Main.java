

public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");

        uneRecherche.retrouve("de hitchcock anderson, avec lamar ou peter, de joseph ");

        uneRecherche.fermeBase();
    }
}