

public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");

        uneRecherche.retrouve("titre avengers ou ");

        uneRecherche.fermeBase();
    }
}