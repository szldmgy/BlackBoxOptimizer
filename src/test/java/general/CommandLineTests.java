package general;

public class CommandLineTests {

    //for(int i = 0; i< args.length;i++)
    //        {
    //            String s = args[i];
    //            if(s.equals("-r"))
    //                inmediateRun[0]=true;
    //            else if(s.equals("-s"))
    //                safeMode[0]=true;
    //            else if(s.equals("-sp")) {
    //                safeMode[0] = true;
    //                savingFrequence[0] = Integer.parseInt(args[++i]);
    //            }
    //            else if(s.equals("-p")) {
    //                customParamFile[0] = true;
    //                customParamFileName[0] = args[i++];
    //            }
    //            else
    //                configFileName[0] = s;
    //        }
//    @Test
//    public void runAll(){
//
//        File[] files = new File("testresults/").listFiles();
//        testFiles(files);
//
//    }
//
//    private void testFiles(File[] files) {
//        Arrays.stream(files).filter(file -> file.getName().contains(".json")).forEach(file -> {
//            File directory = new File("testresults/"+ file.getName().replace(".json",""));
//            if (! directory.exists()){
//                directory.mkdir();
//
//            }
//            String[] args = {"-r","-s",file.getAbsolutePath()};
//            try {
//                Main.optimizer.main(args);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (CloneNotSupportedException e) {
//                e.printStackTrace();
//            }
//            args = new String[]{"-r","-sp","3",file.getAbsolutePath()};
//            try {
//                Main.optimizer.main(args);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (CloneNotSupportedException e) {
//                e.printStackTrace();
//            }
//
//        });
//
//
//    }
    /*@Test
    public void testMain() throws IOException {
        System.out.println("optimizer.main");
        String[] args = null;
        final InputStream original = System.in;
        final FileInputStream fips = new FileInputStream(new File("[path_to_file]"));
        System.setIn(fips);
        Main.optimizer.main(args);
        System.setIn(original);
    }*/
}
