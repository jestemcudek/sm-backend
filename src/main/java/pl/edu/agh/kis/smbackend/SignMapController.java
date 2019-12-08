package pl.edu.agh.kis.smbackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import mil.nga.sf.geojson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.edu.agh.kis.smbackend.dao.GroupDAO;
import pl.edu.agh.kis.smbackend.dao.LocationDAO;
import pl.edu.agh.kis.smbackend.dao.UserDAO;
import pl.edu.agh.kis.smbackend.model.Group;
import pl.edu.agh.kis.smbackend.model.Location;
import pl.edu.agh.kis.smbackend.model.User;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SignMapController {
    Logger logger = LoggerFactory.getLogger(SignMapController.class);

    @Autowired
    UserDAO userDAO;

    @Autowired
    LocationDAO locationDAO;

    @Autowired
    GroupDAO groupDAO;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostMapping(path = "/register", produces = "application/json")
    public ResponseEntity<User> registerUser(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password) {
        User userToAdd = new User(email, firstName, lastName, bCryptPasswordEncoder.encode(password));
        userDAO.saveAndFlush(userToAdd);
        URI uri =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(userToAdd.getId())
                        .toUri();
        return ResponseEntity.created(uri).header("Access-Control-Allow-Origin", "*").body(userToAdd);
    }

    @PostMapping(path = "/login", produces = "application/json")
    public ResponseEntity<User> loginUser(@RequestParam String email, @RequestParam String password) {
        User loggedUser = userDAO.getUserByEmailAndAndPassword(email, bCryptPasswordEncoder.encode(password));
        if (loggedUser == null) {
            return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
        }
        return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").body(loggedUser);
    }

    @PostMapping(path = "/forgotten", produces = "application/json")
    public ResponseEntity<String> remindPassword(@RequestParam String email) {
        String userPwd = userDAO.getUserByEmail(email).getPassword();
        return (userPwd != null)
                ? ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").body(userPwd)
                : ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
    }

    @PostMapping(path = "/password", produces = "application/json")
    public ResponseEntity<String> changePassword(
            @RequestParam String newpass, @RequestParam String oldPass, @RequestParam int userID) {
        User userToChange =
                userDAO.findById(userID).filter(user -> user.getPassword().equals(oldPass)).get();
        if (userToChange != null) {
            userToChange.setPassword(newpass);
            return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").build();
        } else return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
    }

    @PostMapping(path = "/group", produces = "application/json")
    public ResponseEntity<Group> createGroup(
            @RequestParam String name, @RequestParam String usersMails) throws JsonProcessingException {
//        ObjectMapper mapper = new ObjectMapper();
//        List<String> mails = Arrays.asList(mapper.readValue(usersMails, String.class));
        List<String> mails = Arrays.asList(usersMails.split(","));
        List<User> users = new ArrayList<>();
        for (String mail : mails) {
            users.add(userDAO.getUserByEmail(mail));
        }
        Group newGroup = new Group(name, users);
        groupDAO.save(newGroup);
        URI uri =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{groupID")
                        .buildAndExpand(newGroup.getId())
                        .toUri();
        return ResponseEntity.created(uri).header("Access-Control-Allow-Origin", "*").body(newGroup);
    }

    @PostMapping(path = "/selectGroup", produces = "application/json")
    public ResponseEntity<Group> selectGroup(@RequestParam int userID, @RequestParam int groupID) {
        User newMember = null;
        if (userDAO.findById(userID).isPresent() && groupDAO.findById(groupID).isPresent()) {
            newMember = userDAO.findById(userID).get();
            groupDAO.findById(groupID).get().getGroupMembers().add(newMember);
            return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").build();
        } else return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
    }

    @PostMapping(path = "leaveGroup", produces = "application/json")
    public ResponseEntity<Group> leaveGroup(@RequestParam int userID, @RequestParam int groupID) {
        if (!userDAO.findById(userID).isPresent())
            return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();

        groupDAO
                .findById(groupID)
                .ifPresent(group -> group.getGroupMembers().remove(userDAO.findById(userID).get()));
        return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").build();
    }

    @GetMapping(path = "/groups", produces = "application/json")
    public ResponseEntity<List<Group>> getGroupsByUser(@RequestParam int userID) {
        if (!userDAO.findById(userID).isPresent()) return ResponseEntity.notFound().build();
        List<Group> groups =
                groupDAO.findAll().stream()
                        .filter(
                                group -> group.getGroupMembers().stream().anyMatch(user -> user.getId() == userID))
                        .collect(Collectors.toList());
        return ResponseEntity.ok().header("Access-Control-Allow-Origin","*").body(groups);
    }

    @PostMapping(path = "/locations", produces = "application/json")
    public ResponseEntity<List<Location>> getPropositions(@RequestParam double latitude, @RequestParam double longitude) {
        Client client = ClientBuilder.newClient();
        Entity<String> payload = Entity.json("{\"request\":\"pois\", \"geometry\":{\"bbox\":[[19.922776, 50.065689],[19.912776, 50.035689]],\"geojson\":{\"type\":\"Point\", \"coordinates\":[19.922776, 50.065689]},\"buffer\":200}}");
        Response response = client.target("https://api.openrouteservice.org/pois")
                .request()
                .header("Authorization", "5b3ce3597851110001cf624819f9959e642a4232bf8709fe89c37b3b")
                .header("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
                .post(payload);
        String content = response.readEntity(String.class);
        FeatureCollection fc = FeatureConverter.toFeatureCollection(content);
        List<Feature> featureList = fc.getFeatures();
        List<Geometry> geometries = new ArrayList<>();
        featureList.forEach(feature -> geometries.add(feature.getGeometry()));
        List<Point> points = new ArrayList<>();
        geometries.forEach(geometry -> points.add((Point) geometry));
        return ResponseEntity.ok().header("Access-Control-Allow-Origin","*").body(retrieveCoordinates(points));

    }

    private List<Location> retrieveCoordinates(List<Point> points) {
        List<Location> returnValue = new ArrayList<>();
        for (Point pos : points) {
            returnValue.add(new Location(pos.getCoordinates().getY(), pos.getCoordinates().getX()));
        }
        return returnValue;
    }
}
