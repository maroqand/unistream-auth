package uz.upay;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uz.upay.unistream.dto.Client;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@SpringBootApplication
public class UnistreamApplication implements CommandLineRunner {

    @Autowired
    private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(UnistreamApplication.class, args);
    }

    @Override
    public void run(String... args) {

        String appId = "";
        String appSecret = "";
        String posID = "";

        String verb = "GET";
        String contentMD5 = "";
        String rfc_date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
        String pathAndQuery = "/v2/categories?idLevel=1";
        String apiUrlStr = "https://slt-test.api.unistream.com" + pathAndQuery;


        try {
            String data = generateHeaderData(posID, verb, contentMD5, rfc_date, pathAndQuery);

            String signature = encodeHmacSHA256(appSecret, data);
            System.out.println(signature);
            String parameter = "UNIHMAC " + appId + ":" + signature;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", parameter);
            headers.add("Date", rfc_date);

            HttpEntity entity = new HttpEntity(headers);

            ResponseEntity<Client> response = restTemplate.exchange(apiUrlStr, HttpMethod.GET, entity, Client.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String responseString = e.getResponseBodyAsString();
                System.out.println(responseString);

            }
        }

//		clientService.getClient(1);
    }

    public String generateHeaderData(String posID, String verb, String contentMd5, String date, String pathAndQuery) {
        String data = verb.toUpperCase() + "\n"
                + contentMd5 + "\n"
                + date + "\n"
                + pathAndQuery.toLowerCase()
                + "\n" + posID;
        return data;
    }

    //    calculating HmacSHA256 signature
    public String encodeHmacSHA256(String appSecret, String data) {

//        convert the secret key and header data to Base64 with UTF8
        byte[] secretKey = appSecret.getBytes(StandardCharsets.UTF_8);
        byte[] dataByte = data.getBytes(StandardCharsets.UTF_8);

        try {

            String algo = "HmacSHA256";

            SecretKeySpec secret_key = new SecretKeySpec(secretKey, algo);

            Mac sha256_HMAC = Mac.getInstance(algo);
            sha256_HMAC.init(secret_key);

            byte[] result = sha256_HMAC.doFinal(dataByte);

            return Base64.getEncoder().encodeToString(result);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }
}
