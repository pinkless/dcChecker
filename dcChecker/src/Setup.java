import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.*;
import java.util.Properties;

/**
 * ClassName: Setup
 * Package: PACKAGE_NAME
 * Description:
 *
 * @Author: chy
 * @Create: 2024/4/26 - 20:23
 * @Version: v1.0
 */
public class Setup {

    void read(String filepath) {
        File f = new File(filepath);
    }
    public static void split_chunk(String startFilepath,String endFilepath,int chunknum,int pronum,int averagechunknum){
        for(int i=1;i<=pronum;i++){
            for(int j=1;j<=chunknum;j++){
                //源文件路径
                File startFile = new File(startFilepath+j);
                if(j>(i-1)*averagechunknum&&j<=i*averagechunknum&&(chunknum-i*averagechunknum)>=averagechunknum){
                    System.out.println(i+","+j);
                    File endDirection = new File(endFilepath+i);
                    if(!endDirection.exists()){
                        endDirection.mkdirs();
                    }
                    File endFile = new File(endFilepath+i+"/"+j);
                    try{
                        //调用File类的方法renameTO
                        startFile.renameTo(endFile);
                    }catch (Exception e ){
                        System.out.println("文件移动出现异常！");
                    }
                }
                if(j>(i-1)*averagechunknum&&(chunknum-i*averagechunknum)<averagechunknum){
                    System.out.println(i+","+j);
                    File endDirection = new File(endFilepath+i);
                    if(!endDirection.exists()){
                        endDirection.mkdirs();
                    }
                    File endFile = new File(endFilepath+i+"/"+j);
                    try{
                        //调用File类的方法renameTO
                        startFile.renameTo(endFile);
                    }catch (Exception e ){
                        System.out.println("文件移动出现异常！");
                    }
                }
            }
        }

//        for(int i=1;i<=chunknum;i++){
//            //源文件路径
//            File startFile = new File(startFilepath+i);
//            for(int j =1;j<=pronum;j++){
//                if((i>(j-1)*averagechunknum&&i<=j*averagechunknum)&&(chunknum-j*averagechunknum)>=averagechunknum){
//                    File endDirection = new File(endFilepath+j+"/"+i);
//                    if(!endDirection.exists()){
//                        endDirection.mkdirs();
//                    }
//                    try{
//                        //调用File类的方法renameTO
//                        startFile.renameTo(endDirection);
//                    }catch (Exception e ){
//                        System.out.println("文件移动出现异常！");
//                    }
//                }
//            }
//        }

    }


    public static void main(String[] args) {
        String dir = "data/";
        String schunkPath = dir + "datachunk/";
        String echunkPath = "StorageProvider/datachunk";
        int chunknum = 2560;
        int pronum = 20;
        int averagechunknum = 128;
        split_chunk(schunkPath,echunkPath,chunknum,pronum,averagechunknum);










//        String Filepath = "D:\\programtool\\code\\idea\\Decentralized_storage_auditor\\123.txt";
//        File f = new File(Filepath);
////        System.out.println(f.exists());
//
//        Pairing bp = PairingFactory.getPairing("a.properties");
//        Field G1 = bp.getG1();
//        Field Zr = bp.getZr();
//        Element m[] = new Element[400];
//        Element g = G1.newRandomElement();
//        Element x = Zr.newRandomElement();
//        System.out.println(g);
//        try(InputStream inputStream = new FileInputStream(Filepath)){
//            byte[] tmp = null;
//            StringBuilder stringBuilder = new StringBuilder();
//            int i = 0;
//            while(true){
//                byte[] buffer = new byte[20];
//                int len = inputStream.read(buffer);
//                if(len == -1){
//                    break;
//                }
//                tmp = buffer;
//                System.out.println(tmp);
//                m[i] = Zr.newElementFromHash(tmp,0,tmp.length);
//                i++;
////                System.out.println(m[i]);
////                System.out.println(buffer);
////                System.out.println(i);
//                stringBuilder.append(tmp);
//            }
////            System.out.println(stringBuilder);
//        }catch(IOException e){
//            e.printStackTrace();
//        };
//
//
//        Element accB = g.duplicate().powZn(m[0]);
//        for(int i =0;m[i]!=null;i++)
//        {
//            if(i>0){
//                accB = accB.duplicate().powZn(m[i]);
//            }
//            System.out.println(i);
//            System.out.println(m[i]);
//            System.out.println(accB);
//        }
//        System.out.println(accB);
    }
}
