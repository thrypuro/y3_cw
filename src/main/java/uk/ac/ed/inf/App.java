package uk.ac.ed.inf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * The main method where the drone parses the arguments, and it is started!
 */
public class App
{
    /**
     * The main function of the program
     * @param args the arguments given to the program
     */
    public static void main( String[] args )
    {   long d1=System.currentTimeMillis();
        System.out.println(" .........DRONE START..........");
        // User arguments
        String machine = "127.0.0.1";
        String port1 = args[3];
        String port2 = args[4];
        String day = args[0];
        String month = args[1];
        String year = args[2];
        System.out.println("Computing orders for the day:  " + day +"/"+month+"/"+year);
        // Arguments are zipped in a ArrayList to save space
        String[] arguments = {machine,port1,port2,day,month,year};

        // Stage 1 : Initialise all the necessary components
        // Make sure all the data given are correct before going through next stage

        // Constructor for Initialise Class
        Initialise I = new Initialise(arguments);
        System.out.println();
        System.out.println("## Step 1 Complete : Initiated !");
        System.out.println();
        // Stage 2 : Compute the Path
        Compute C = new Compute(I);

        System.out.println("Path has been computed, writing results to file and database!");

        // Stage 3 : Commit and Write the necessary data

        /* Filename that the geojson result is written to
         filename is constructed from the user argument
         */
        String filename = "drone-"+day+"-"+month+"-"+year+".geojson";
        String final_geojson=Internal_path.make_geo_json(Compute.Path);
        try {
            // create a file and write to it
            FileOutputStream outputStream = new FileOutputStream(filename);
            byte[] strToBytes = final_geojson.getBytes();
            outputStream.write(strToBytes);
            outputStream.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        System.out.println();
        System.out.println("## Step 2 Complete : Results have been Computed!");
        System.out.println();
        System.out.println("Results written to a geojson File with filename:-  "+filename  );
        /* We need both the permutation and Customers in order
        write to the database, almost all the info needed are stored
        in this neatly.
        Deliveries Columns :
          Order-no | Delivered-To | Cost
        Flightpath Columns :
          Order-no | Old-Longitude | Old-Latitude | angle | New-Longitude | New-Latitude
         */
        ArrayList<Internal_path> Customers = C.getCustomers();
        List<Integer> Perm = C.getExternal_Permutation();
        /* Write to deliveries and flightpath, the Last permutation/customer will always be AT hence we should not
        write that to the deliveries table, and it specifies AT in flightpath.
         */
        for (int j = 0;j< Perm.size();j++) {
            int perm =  Perm.get(j) - 1;
            Vector<LongLat> V = Customers.get(perm).Path;
            String orderno = Customers.get(perm).order_no;
            // if it's an order, a valid orderno, that is not returning to AT
            if(perm<Perm.size()-1){
                I.getWeb().write_deliveries_table(Customers.get(perm).order_no,Customers.get(perm).destination_location
                        ,String.valueOf(Customers.get(perm).cost),"insert into deliveries values (?, ?, ?)");
            }
            for (int i = 0; i <V.size()-1; i++) {
                int angl = V.get(i).angle(V.get(i+1));
                // Insert the Data into the Flightpath table
                I.getWeb().write_flightpath_table("flightpath","insert into flightpath values (?, ?, ?, ?, ?, ?)"
                        ,orderno
                        ,String.valueOf(V.get(i).longitude),String.valueOf(V.get(i).latitude),String.valueOf(angl),
                        String.valueOf(V.get(i+1).longitude),String.valueOf(V.get(i+1).latitude));

            }
            // if it is not the last path, since if it is the last path, there doesn't exist a customer since (j+1)>Perm.size-1 hence out of bounds
            if(j<Perm.size()-1){
                // if it is not the Last Path (Appleton Tower)
                int perm2 =  Perm.get(j+1) - 1;
                int angl = V.get(V.size()-1).angle(Customers.get(perm2).Path.get(0));
                I.getWeb().write_flightpath_table("flightpath"
                        ,"insert into flightpath values (?, ?, ?, ?, ?, ?)"
                        ,orderno
                        ,String.valueOf(V.get(V.size()-1).longitude),String.valueOf(V.get(V.size()-1).latitude)
                        ,(String.valueOf(angl)),
                        String.valueOf(Customers.get(perm).Path.get(0).longitude)
                        ,String.valueOf(Customers.get(perm).Path.get(0).latitude));
            }
        }
        System.out.println("Results written to the database.");
        System.out.println();
        System.out.println("## Step 3 Complete : Committed and written!");
        System.out.println();
        System.out.println(" .............DRONE END..............");

        long d2 = System.currentTimeMillis();
        System.out.println( "Time taken : "+((d2 - d1)/1000)+"s");
    }
}
