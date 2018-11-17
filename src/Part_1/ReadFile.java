package Part_1;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.util.StringTokenizer;

public class ReadFile {

    public void readThroughFiles(String dirPath) throws FileNotFoundException {
        FastReader reader;
        File dir = new File(dirPath);
        File[] subDirs = dir.listFiles();
        if (subDirs != null) {
            for (File f : subDirs) {
                String docText;
                File[] tempFiles = f.listFiles();
                if (tempFiles != null) {
                    try {
                        Document document = Jsoup.parse(new String(Files.readAllBytes(f.toPath())));
                        Elements elements = document.getElementsByTag("DOC");
                        for (Element element : elements) {
                            String name = element.getElementsByTag("TEXT").text();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    reader = new FastReader(tempFiles[0]);

                }
            }
        }
    }

    static class FastReader
    {
        BufferedReader br;
        StringTokenizer st;

        public FastReader(File file) throws FileNotFoundException {
            br = new BufferedReader(new FileReader(file));
        }

        String next()
        {
            while (st == null || !st.hasMoreElements())
            {
                try
                {
                    st = new StringTokenizer(br.readLine());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            return st.nextToken();
        }

        int nextInt()
        {
            return Integer.parseInt(next());
        }

        long nextLong()
        {
            return Long.parseLong(next());
        }

        double nextDouble()
        {
            return Double.parseDouble(next());
        }

        String nextLine()
        {
            String str = "";
            try
            {
                str = br.readLine();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return str;
        }
    }
}
