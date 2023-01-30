import java.io.*;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

public class Parser {
    private static final String DEFAULT_NAME_FILE = "acronyms.csv";
    private final File file;

    public Parser() {
        file = new File(DEFAULT_NAME_FILE);
    }

    public Parser(String nameFile) {
        this.file = nameFile.endsWith(".csv") ? new File(nameFile) : new File(nameFile + ".csv");
    }


    public void parse() {
        try (FileWriter out = new FileWriter(file); PrintWriter pw = new PrintWriter(out)) {
            //Getting all links to pages with acronyms, without duplicates
            Elements link = getPagesLinksObjectsAcronyms();
            //Getting all links to acronyms
            Elements linkAcr = getLinksObjectsAcronyms(link);
            //Getting all links to attributes acronyms and write they to file
            writeInformation(linkAcr, pw);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private Elements getPagesLinksObjectsAcronyms() throws IOException {
        Document doc = Jsoup.connect("https://www.teledynecaris.com/s-57/frames/main.html").get();
        Elements linksPages = new Elements(doc.getElementsByTag("a")
                .stream()
                .skip(1)    //Skip first element because it does not match the task
                .takeWhile(x -> !x.attr("href").equals("../attribut/attrib_info.htm"))
                .collect(Collectors.toList())
        );
        //Delete duplicate pages
        for (int i = 0; i < linksPages.size() - 1; i++) {
            if (linksPages.get(i).attr("href").equals(linksPages.get(i + 1).attr("href"))) {
                linksPages.remove(i);
                i--;
            }
        }
        return linksPages;
    }

    private Elements getLinksObjectsAcronyms(Elements pages) throws IOException {
        Elements links = new Elements();
        return pages.stream().flatMap(p -> Jsoup.connect(p.absUrl("href")).get().getElementsByTag("a")
                .stream()
                .skip(1)
                .collect(Collectors.toList())).collect(Collectors.toList());

        /*for (Element page : pages) {
            Elements link = Jsoup.connect(page.absUrl("href")).get().getElementsByTag("a");
            link.remove(0);
            links.addAll(link);
        }*/
        return links;
    }

    private void writeInformation(Elements pages, PrintWriter pw) throws IOException, InterruptedException {
        for (Element page : pages) {
            //Thread.sleep(1);
            Document doc = Jsoup.connect(page.absUrl("href")).get();
            Elements link = doc.getElementsByTag("p");
            String temp = "\"";
            String curr = "";
            for (Element el : link) {
                if (el.text().matches("\\s*Object Class: .+")) {
                    curr = el.text().substring(14) + "\",\"";
                }
                if (el.text().matches("\\s*Acronym: .+")) {
                    temp += el.text().substring(9) + "\",\"";
                    break;
                }
            }
            temp += curr;
            for (Element el : doc.getElementsByTag("a")) {
                //Thread.sleep(1);
                writeFromAttributePage(Jsoup.connect(el.absUrl("href")).get(), temp, pw);
            }
        }
    }

    private void writeFromAttributePage(Document page, String temp, PrintWriter pw) {
        String curr = "";
        for (Element el : page.getElementsByTag("p")) {
            if (el.text().matches("\\s*Attribute: .+")) {
                curr = el.text().substring(11) + "\",\"";
            } else if (el.text().matches("\\s*Acronym: .+")) {
                temp += el.text().substring(9) + "\",\"";
                break;
            }
        }
        for (Element el : page.getElementsByTag("h2")) {
            if (el.text().matches("\\s*Attribute: .+")) {
                curr = el.text().substring(11) + "\",\"";
            } else if (el.text().matches("\\s*Acronym: .+")) {
                temp += el.text().substring(9) + "\",\"";
                break;
            }
        }
        temp += curr;
        Elements links = page.getElementsByTag("table");
        if (links.isEmpty()) {
            pw.write(temp + "\"\n");
            return;
        } else {
            links = links.get(0).getElementsByTag("td");
        }

        for (int i = 1; i < links.size(); i += 4) {
            pw.write(temp + links.get(i).text() + "\"\n");
        }
    }
}
