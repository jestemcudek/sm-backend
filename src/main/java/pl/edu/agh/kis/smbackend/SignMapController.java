package pl.edu.agh.kis.smbackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import mil.nga.sf.geojson.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

@RestController
public class SignMapController {

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
        //User userToAdd = new User(email, firstName, lastName, password);
        userDAO.saveAndFlush(userToAdd);
        URI uri =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(userToAdd.getId())
                        .toUri();
        return ResponseEntity.created(uri).body(userToAdd);
    }

    @PostMapping(path = "/login", produces = "application/json")
    public ResponseEntity<User> loginUser(@RequestParam String email, @RequestParam String password) {
        User loggedUser = userDAO.getUserByEmailAndAndPassword(email, bCryptPasswordEncoder.encode(password));
        //User loggedUser = userDAO.getUserByEmailAndAndPassword(email, password);
        if (loggedUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(loggedUser);
    }

    @PostMapping(path = "/forgotten", produces = "application/json")
    public ResponseEntity<String> remindPassword(@RequestParam String email) {
        String userPwd = userDAO.getUserByEmail(email).getPassword();
        return (userPwd != null)
                ? ResponseEntity.ok().body(userPwd)
                : ResponseEntity.notFound().build();
    }

    @PostMapping(path = "/password", produces = "application/json")
    public ResponseEntity<String> changePassword(
            @RequestParam String newpass, @RequestParam String oldPass, @RequestParam int userID) {
        User userToChange =
                userDAO.findById(userID).filter(user -> user.getPassword().equals(oldPass)).get();
        if (userToChange != null) {
            userToChange.setPassword(newpass);
            return ResponseEntity.ok().build();
        } else return ResponseEntity.notFound().build();
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
                        .path("/{groupID}")
                        .buildAndExpand(newGroup.getId())
                        .toUri();
        return ResponseEntity.created(uri).body(newGroup);
    }

    @PostMapping(path = "/selectGroup", produces = "application/json")
    public ResponseEntity<Group> selectGroup(Authentication authentication, @RequestParam int groupID) {
        User newMember = null;
        if (groupDAO.findById(groupID).isPresent()) {
            newMember = userDAO.getUserByEmail(authentication.getPrincipal().toString());
            Group g = groupDAO.findById(groupID).get();
            g.getGroupMembers().add(newMember);
            groupDAO.saveAndFlush(g);
            return ResponseEntity.ok().build();
        } else return ResponseEntity.notFound().build();
    }

    @PostMapping(path = "/leaveGroup", produces = "application/json")
    public ResponseEntity<Group> leaveGroup(Authentication authentication, @RequestParam int groupID) {
        User user = userDAO.getUserByEmail(authentication.getPrincipal().toString());

        groupDAO
                .findById(groupID)
                .ifPresent(group -> {
                    group.getGroupMembers().remove(user);
                    groupDAO.saveAndFlush(group);
                });
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/groups", produces = "application/json")
    public ResponseEntity<List<Group>> getGroupsByUser(Authentication authentication) {
        User user = userDAO.getUserByEmail(authentication.getPrincipal().toString());
        if (user == null) return ResponseEntity.notFound().build();
        List<Group> groups = new ArrayList<>();
        groupDAO.findAll().forEach(group -> {
            if (group.getGroupMembers().contains(user))
                groups.add(group);
        });
        return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").body(groups);
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
        featureList.forEach(feature -> {
            geometries.add(feature.getGeometry());
            System.out.println(feature.getProperties().get(""));
        });
        List<Point> points = new ArrayList<>();
        geometries.forEach(geometry -> points.add((Point) geometry));
        //return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").body(retrieveCoordinates(points));
        return ResponseEntity.status(HttpStatus.OK).body(retrieveCoordinates(points));
    }

    @PostMapping(path = "/setLocation", produces = "application/json")
    public ResponseEntity setLocation(@RequestParam double latitude, @RequestParam double longtitude, Authentication authentication) {
        Location loc = locationDAO.findByLatitudeAndLongitude(latitude, longtitude).orElseGet(() -> {
            Location tmp = new Location(latitude, longtitude);
            locationDAO.saveAndFlush(tmp);
            return tmp;
        });
        User user = userDAO.getUserByEmail(authentication.getPrincipal().toString());
        user.setCurrentLocation(loc);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/settings", produces = "application/json")
    public ResponseEntity getFullData(Authentication authentication) {
        User user = userDAO.getUserByEmail(authentication.getPrincipal().toString());
        if (user == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    private List<Location> retrieveCoordinates(List<Point> points) {
        List<Location> returnValue = new ArrayList<>();
        if (!points.isEmpty()) {
            for (Point pos : points) {
                returnValue.add(new Location(pos.getCoordinates().getY(), pos.getCoordinates().getX()));
            }
        } else
            returnValue = localSuggestions();
        return returnValue;
    }

    private List<Location> localSuggestions() {
        List<Location> reserveList = new ArrayList<>();
        reserveList.add(new Location("Grilosfera", 50.068473, 19.905997));
        reserveList.add(new Location("Wejście do Lewiatana MS AGH", 50.068494, 19.907510));
        reserveList.add(new Location("Orlik AGH", 50.068280, 19.904633));
        reserveList.add(new Location("Przystanek MS AGH", 50.069599, 19.906463));
        reserveList.add(new Location("WZ AGH", 50.070385, 19.906340));
        reserveList.add(new Location("B1 AGH", 50.065806, 19.919285));
        reserveList.add(new Location("Plac Inwalidów", 50.069504, 19.925571));
        reserveList.add(new Location("Fontanna przy UR", 50.062878, 19.922719));
        reserveList.add(new Location("Dolne Młyny - Weźże Krafta", 50.064736, 19.926079));
        reserveList.add(new Location("AGH D17", 50.068086, 19.912649));
        return reserveList;
    }
}
