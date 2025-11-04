package de.twonirwana.infinity;


import java.io.IOException;

public class ExportAll {

    public static void main(String[] args) throws IOException {
        Database db = new DatabaseImp();

        db.getAllHackingPrograms().forEach(System.out::println);

    }


}
