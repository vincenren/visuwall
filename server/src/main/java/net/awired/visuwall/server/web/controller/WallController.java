package net.awired.visuwall.server.web.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import net.awired.visuwall.api.domain.Project;
import net.awired.visuwall.api.domain.ProjectStatus;
import net.awired.visuwall.server.domain.Software;
import net.awired.visuwall.server.domain.SoftwareAccess;
import net.awired.visuwall.server.domain.Wall;
import net.awired.visuwall.server.service.WallService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/wall")
public class WallController {

	private static final Logger LOG = LoggerFactory.getLogger(WallController.class.getName());
	private static final String WALL_JSP = "wall/wall";

	@Autowired
	private WallService wallService;
	
    Wall wall;
	
    public WallController() {
        wall = new Wall("orange-vallee");
//                wall.addSoftwareAccess(new SoftwareAccess(Software.HUDSON, "http://integration.wormee.orange-vallee.net:8080/hudson"));
//                wall.addSoftwareAccess(new SoftwareAccess(Software.SONAR, "http://integration.wormee.orange-vallee.net:9000"));
//        wall.addSoftwareAccess(new SoftwareAccess(Software.HUDSON, "http://ci.awired.net/jenkins"));
        wall.addSoftwareAccess(new SoftwareAccess(Software.HUDSON, "http://ci.visuwall.awired.net"));
        wall.addSoftwareAccess(new SoftwareAccess(Software.SONAR, "http://sonar.awired.net"));
        // wall.addSoftwareAccess(new SoftwareAccess(Software.HUDSON, "http://fluxx.fr.cr:8080/hudson"));
        // wall.addSoftwareAccess(new SoftwareAccess(Software.SONAR, "http://fluxx.fr.cr:9000"));
        wall.refreshProjects();
    }

    @PostConstruct
    public void postConstruct() {
        wallService.addWall(wall);
    }
    
    ////////////////////////////////////////////////////////////////////
	
	@RequestMapping
	public String getWallNames(ModelMap model) {
		model.put("data", wallService.getWallNames());
		return WALL_JSP;
	}

    @RequestMapping("{wallName}")
    public Wall getProjects(@PathVariable String wallName) {
    	Wall wall = wallService.getWall(wallName);
        LOG.info("Projects collection size :" + wall.getProjects().size());
        return wall;
    }
	
    @RequestMapping("{wallName}/status")
    public @ResponseBody List<ProjectStatus> getStatus(@PathVariable String wallName) {
        return wallService.getStatus(wallName);
    }

    
  //  $.ajax("wall/create", {contentType: "application/json", dataType: "json", success : function(data) {alert(data);});
    
    
	@RequestMapping(value = "create", method = RequestMethod.GET)
	public String getCreate(ModelMap model) {
        model.addAttribute("data", new Wall());
		return WALL_JSP;
	}

	@RequestMapping(value = "create", method = RequestMethod.POST)
	public ModelAndView create() {
		return null;
	}
	
	@RequestMapping(value = "{wallName}", method = RequestMethod.DELETE)
	public void DeleteWall(@PathVariable String wallName) {
		
	}

}
