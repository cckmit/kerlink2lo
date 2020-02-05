package com.orange.lo.sample.kerlink2lo.lo;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LoDeviceProvider {
    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RestTemplate restTemplate;
    private LoProperties loProperties;
    private HttpHeaders authenticationHeaders;
    private HttpEntity<?> entity;
    private String queriedField = "properties";
    private String url;

    @Autowired
    public LoDeviceProvider(LoProperties loProperties, HttpHeaders authenticationHeaders) {
        this.restTemplate = new RestTemplate();
        this.loProperties = loProperties;
        this.authenticationHeaders = authenticationHeaders;
        entity = new HttpEntity<>(authenticationHeaders);
        url = loProperties.getDevicesUrl() +
                "?limit=" + loProperties.getPageSize() +
                "&offset=" + "%1$s" +
                "&filterQuery=" + queriedField +
                "." + loProperties.getPropertiesKey() +
                "==" + loProperties.getPropertiesValue() +
                "&fields=" + queriedField;
    }

    public List<LoDevice> getAllDevices() {
        ResponseEntity<LoDevice[]> response = restTemplate.exchange(loProperties.getDevicesUrl(), HttpMethod.GET, entity, LoDevice[].class);
        return Arrays.asList(response.getBody());
    }

    public List<LoDevice> getDevices() {
        List<LoDevice> devices = new ArrayList<>(loProperties.getPageSize());
        for (int offset = 0; ; offset++) {
            LOG.info(getUrl(offset));
            ResponseEntity<LoDevice[]> response = restTemplate.exchange(getUrl(offset), HttpMethod.GET, entity, LoDevice[].class);
            LOG.info(offset + " " + response.getBody().length);
            if (response.getBody().length == 0)
                break;
            for (LoDevice device : Arrays.asList(response.getBody()))
                LOG.info(device.toString());
            LOG.info("\n");
            devices.addAll(Arrays.asList(response.getBody()));
            if (response.getBody().length < loProperties.getPageSize())
                break;
        }
        LOG.info("Devices: " + devices.toString());
        return devices;
    }

    public void addDevice(String deviceId, String deviceName, Map<String, String> properties) {
        LoDevice loDevice = new LoDevice(deviceId, deviceName, properties);
        entity = new HttpEntity<LoDevice>(loDevice, authenticationHeaders);
        LOG.info(loDevice.toString());
        ResponseEntity<String> response = restTemplate.exchange(loProperties.getDevicesUrl(), HttpMethod.POST, entity, String.class);
        LOG.info(response.toString());
        ObjectMapper om = new ObjectMapper();
        try {
            String writeValueAsString = om.writeValueAsString(loDevice);
            System.out.println("***");
            System.out.println(writeValueAsString);
            System.out.println("***");
            // {"id":"urn:lo:nsid:sensor:temp10","name":"temp10","platform":"kerlink"}
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void deleteDevice(String deviceId) {
        ResponseEntity<String> response = restTemplate.exchange(loProperties.getDevicesUrl() + "/" + deviceId, HttpMethod.DELETE, entity, String.class);
    }

    private String getUrl(int offset) {
        return String.format(url, offset * loProperties.getPageSize());
    }
}