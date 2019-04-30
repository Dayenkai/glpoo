

public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");

        uneRecherche.fermeBase();

        uneRecherche.retrouve("DE Hitchcock, AVEC MattPokora");
    }
}