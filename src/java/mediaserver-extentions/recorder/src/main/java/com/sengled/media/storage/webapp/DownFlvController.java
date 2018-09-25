package com.sengled.media.storage.webapp;

import java.io.File;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.sengled.media.storage.s3.AmazonS3Template;

@Controller
public class DownFlvController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownFlvController.class);


    @Autowired
    AmazonS3Template s3Template;

    @Value("${aws_video_bucket}")
    private String awsVideoBucket;


    @RequestMapping(value = "/downflvvideo/{prefix}/{filename}")
    @ResponseBody
    public void down(@PathVariable String prefix, @PathVariable String filename,
            HttpServletRequest request, HttpServletResponse response) {
        // http://localhost:8888/downflvvideo/B14/B14EB349-1D85-4EB0-B9F4-FCA30139F945.dat?4690290-8626670
        if (!filename.substring(0, 3).equals(prefix)) {
            return;
        }
        OutputStream out = null;
        String ur = request.getRequestURI();
        File file = new File(ur);
        String key = file.getName();
        String query = request.getQueryString();
        S3ObjectInputStream content = null;
        try {
            String[] range = query.split("-");
            Long p0 = Long.valueOf(range[0]);
            Long p1 = Long.valueOf(range[1]);
            GetObjectRequest getObjectRequest = new GetObjectRequest(awsVideoBucket, key);
            getObjectRequest.setRange(p0, p1);
            S3Object obj = s3Template.getObject(getObjectRequest);
            out = response.getOutputStream();

            response.setContentType("video/x-flv");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;filename=" + key + ".flv");
            content = obj.getObjectContent();
            IOUtils.copy(content, out);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(content);
            IOUtils.closeQuietly(out);
        }
    }
}

