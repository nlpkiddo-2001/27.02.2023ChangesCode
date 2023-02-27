//$Id$


import java.util.ArrayList;
import java.util.List;

public class NEREntities {

    private List<String> location = new ArrayList<>();
    private List<String> participants = new ArrayList<>();

    public NEREntities() {
    }

    public NEREntities(List<String> location, List<String> participants) {
        this.location = location;
        this.participants = participants;
    }

    public List<String> getLocation() {
        return location;
    }

    public void setLocation(List<String> location) {
        this.location = location;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }
}

