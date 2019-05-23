import java.util.*;


public class Main {

    public static void main(String[] args) {
        RechercheFilm uneRecherche = new RechercheFilm("bdfilm");
        //Quand on a pas d'informations sur la transition entre le keyword suivant, on met un ou ?
        uneRecherche.retrouve(" APRES 1998, 1999 ou 2000");
     
        String[] arr = "Joseph Banzio, Anthony Marchal ou Stephen Hawking Christian,".split("ou|,");
        
        ArrayList<List<String>> names= new ArrayList<List<String>>();
        
        for(String n : arr){
            names.add(Arrays.asList(n.split(" ")));
        }

        System.out.println(names);
        long debut = System.currentTimeMillis();

        System.out.println("Temps d'exécution = " + (System.currentTimeMillis()-debut) + " ms");
    }

   
}