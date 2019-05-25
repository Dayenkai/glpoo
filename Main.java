
public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm.sqlite");
        //Quand on a pas d'informations sur la transition entre le keyword suivant, on met un ou ?
        System.out.println(uneRecherche.retrouve("titre avengers ou avec mclane"));
        //String dataName = new StringBuilder("mclane").insert(1,"a").toString();
        //System.out.println(dataName);
        uneRecherche.fermeBase();
        long debut = System.currentTimeMillis();
        System.out.println("Temps d'exécution = " + (System.currentTimeMillis()-debut) + " ms");
    }
   
}