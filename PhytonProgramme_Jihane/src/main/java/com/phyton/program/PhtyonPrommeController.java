package com.phyton.program;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import javax.websocket.server.PathParam;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.python.google.common.base.Charsets;
import org.python.google.common.io.Files;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.phyton.program.entity.Python;

@RestController
public class PhtyonPrommeController {
	
	private final List<Python> pythons = new ArrayList<>();
	private final List<Long> sessionIds = new ArrayList<>();
	
	@RequestMapping(value = "/execute", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Python> executePhyton(@RequestBody Python python) {
		// add sessionId to know if we should renisialize the varriable
		Long sessionId = python.getSessionId();
		// get the content of the code value
		JSONObject json = (JSONObject) JSONSerializer.toJSON(python);
		String cmdPhyton = json.getString("code");

		// create a new file .py if not exist and if this is a new user
		String content = null;
		File file = null;
		try {
			file = new File("pyhton.py");
			if (!file.exists() ||( sessionId !=null && !sessionIds.contains(sessionId))) {
				file.createNewFile();
			}else {
				content = Files.toString(file, Charsets.UTF_8);
			}
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		// write into the file .py
		FileWriter writer;
		try {
			writer = new FileWriter("pyhton.py");
			writer.write(content != null && !content.isEmpty() ? content + ";"
					: "");
			writer.write(cmdPhyton.split(" ", 2)[1]);
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		StringWriter output = new StringWriter();
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptContext context = new SimpleScriptContext();
		context.setWriter(output);
		ScriptEngine engine = manager
				.getEngineByName(cmdPhyton.split(" ", 2)[0].replace("%", ""));
		try {
			engine.eval(new FileReader(file.getPath()), context);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		if(sessionId !=null){
			sessionIds.add(sessionId);
			}
		
		System.out.println(output.toString());

		Python result = new Python();
		result.setResult(output.toString());

		pythons.add(result);
		return pythons;
	}
}	