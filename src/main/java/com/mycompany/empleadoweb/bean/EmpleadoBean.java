/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.empleadoweb.bean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.empleadoweb.dto.Area;
import com.mycompany.empleadoweb.dto.Empleado;
import com.mycompany.empleadoweb.dto.Pais;
import com.mycompany.empleadoweb.dto.TipoDocumento;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.primefaces.PrimeFaces;

/**
 *
 * @author OlartePlata
 */
public class EmpleadoBean implements Serializable {

    private Empleado empleado;
    private List<Empleado> empleados;
    private List<TipoDocumento> tipoDocumentos;
    private List<Pais> paises;
    private List<Area> areas;
    private Date fechaMin;
    private Date fechaMax;
    private static final Long ID_PAIS_COLOMBIA = 1L;

    @PostConstruct
    public void iniciar() {
        crear();
        cargarEmpleados();
        cargarPaises();
        cargarArea();
        cargarTipoDocumento();
    }

    private void cargarEmpleados() {
        Client cliente = ClientBuilder.newClient();
        String respuesta = cliente.target("http://localhost:8080/empleado/listar").
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                get(String.class);
        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        Gson gson = builder.create();
        empleados = gson.fromJson(respuesta, new TypeToken<List<Empleado>>() {
        }.getType());
    }

    private void cargarPaises() {
        Client cliente = ClientBuilder.newClient();
        String respuesta = cliente.target("http://localhost:8080/empleado/listarpaises").
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                get(String.class);
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        paises = gson.fromJson(respuesta, new TypeToken<List<Pais>>() {
        }.getType());
    }

    private void cargarArea() {
        Client cliente = ClientBuilder.newClient();
        String respuesta = cliente.target("http://localhost:8080/empleado/listarareas").
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                get(String.class);
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        areas = gson.fromJson(respuesta, new TypeToken<List<Area>>() {
        }.getType());
    }

    private void cargarTipoDocumento() {
        Client cliente = ClientBuilder.newClient();
        String respuesta = cliente.target("http://localhost:8080/empleado/listartipodocumentos").
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                get(String.class);
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        tipoDocumentos = gson.fromJson(respuesta, new TypeToken<List<TipoDocumento>>() {
        }.getType());
    }

    public void armarCorreo() {
        String correo = "";
        correo = correo.concat(empleado.getPrimerNombre()).
                concat(".").
                concat(empleado.getPrimerApellido());

        //Busca empleados con el mismo nombre
        Client cliente = ClientBuilder.newClient();
        String respuesta = cliente.target("http://localhost:8080/empleado/correo/" + correo).
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                get(String.class);
        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        Gson gson = builder.create();
        List<Empleado> empleadosCorreo = gson.fromJson(respuesta, new TypeToken<List<Empleado>>() {
        }.getType());

        if (!empleadosCorreo.isEmpty()) {
            correo = correo.concat(".").
                    concat(String.valueOf(empleadosCorreo.size() + 1));
        }

        if (empleado.getPais().getId() == ID_PAIS_COLOMBIA) {
            correo = correo.concat("@cidenet.com.co");
        } else {
            correo = correo.concat("@cidenet.com.us");
        }

        empleado.setCorreoElectronico(correo);

    }

    public List<Empleado> consultarDocumento() {
        //Busca empleados con el mismo documento
        Client cliente = ClientBuilder.newClient();
        String respuesta = cliente.target("http://localhost:8080/empleado/consultardocumento/" + empleado.getNumeroIdentificacion()).
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                get(String.class);
        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        Gson gson = builder.create();
        List<Empleado> empleadosDocumento = gson.fromJson(respuesta, new TypeToken<List<Empleado>>() {
        }.getType());
        
        return empleadosDocumento;
    }

    public void crear() {
        empleado = new Empleado();
        empleado.setTipoDocumento(new TipoDocumento());
        empleado.setArea(new Area());
        empleado.setPais(new Pais());
        fechaMin = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -30);
        fechaMin.setTime(calendar.getTime().getTime());
        fechaMax = new Date();

    }

    public void guardar() {
        armarCorreo();
        FacesContext fc = FacesContext.getCurrentInstance();
        
        if (!consultarDocumento().isEmpty()) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Información", "Ya existe un empleado con el número de identificación : " + empleado.getNumeroIdentificacion()));
            return;
        }
        try {
            empleado.setEstado(true);
            empleado.setFechaHoraRegistro(new Date());
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();

            Client cliente = ClientBuilder.newClient();
            WebTarget webTarget = cliente.target("http://localhost:8080/empleado/guardar").path("");
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
            Response response = invocationBuilder.post(Entity.entity(empleado, MediaType.APPLICATION_JSON));
            if (response.getStatus() == 200) {
                String respuesta = response.readEntity(String.class);
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Información", respuesta));
                PrimeFaces.current().executeScript("PF('dlgCrearWv').hide();");
                cargarEmpleados();
            } else {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Información", "Ocurrío un error al momento de guardar"));
            }

        } catch (Exception ex) {
            Logger.getLogger(EmpleadoBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void modificar() {
        FacesContext fc = FacesContext.getCurrentInstance();
        try {
            empleado.setFechaHoraEdicion(new Date());
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();

            Client cliente = ClientBuilder.newClient();
            Response response = cliente.target("http://localhost:8080/empleado/modificar").
                    request().
                    put(Entity.entity(empleado, MediaType.APPLICATION_JSON));
            if (response.getStatus() == 200) {
                String respuesta = response.readEntity(String.class);
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Información", respuesta));
                PrimeFaces.current().executeScript("PF('dlgEditarWv').hide()");
            } else {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Información", "Ocurrío un error al momento de guardar"));
            }

        } catch (Exception ex) {
            Logger.getLogger(EmpleadoBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void eliminar() {
        FacesContext fc = FacesContext.getCurrentInstance();
        Client cliente = ClientBuilder.newClient();
        Response response = cliente.target("http://localhost:8080/empleado/borrar/" + empleado.getId()).
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                delete();

        if (response.getStatus() == 200) {
            String respuesta = response.readEntity(String.class);
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Información", respuesta));
            cargarEmpleados();
        } else {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Información", "Ocurrío un error al momento de eliminar"));
        }
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public List<Empleado> getEmpleados() {
        return empleados;
    }

    public void setEmpleados(List<Empleado> empleados) {
        this.empleados = empleados;
    }

    public List<TipoDocumento> getTipoDocumentos() {
        return tipoDocumentos;
    }

    public void setTipoDocumentos(List<TipoDocumento> tipoDocumentos) {
        this.tipoDocumentos = tipoDocumentos;
    }

    public List<Pais> getPaises() {
        return paises;
    }

    public void setPaises(List<Pais> paises) {
        this.paises = paises;
    }

    public List<Area> getAreas() {
        return areas;
    }

    public void setAreas(List<Area> areas) {
        this.areas = areas;
    }

    public Date getFechaMin() {
        return fechaMin;
    }

    public void setFechaMin(Date fechaMin) {
        this.fechaMin = fechaMin;
    }

    public Date getFechaMax() {
        return fechaMax;
    }

    public void setFechaMax(Date fechaMax) {
        this.fechaMax = fechaMax;
    }

}
