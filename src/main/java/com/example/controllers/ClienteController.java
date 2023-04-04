package com.example.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.entities.Cliente;
import com.example.entities.FileUploadUtil;
import com.example.entities.Mascota;
import com.example.model.FileUploadResponse;
import com.example.services.ClienteService;
import com.example.services.MascotaService;
import com.example.utilities.FileDownloadUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {
    
    private final FileDownloadUtil fileDownloadUtil;

   @Autowired
    private ClienteService clienteService;

    @Autowired
    private FileUploadUtil fileUploadUtil;
    @Autowired
    private MascotaService mascotaService;


    @GetMapping
    public ResponseEntity<List<Cliente>> findAll(@RequestParam(name = "page", required = false) Integer page,
                                                @RequestParam(name = "size", required = false) Integer size) {

        ResponseEntity<List<Cliente>> responseEntity = null;
        List<Cliente> clientes = new ArrayList<>();
        Sort sortByNombre = Sort.by("nombre");


        if (page != null && size != null) {

            try {

                Pageable pageable = PageRequest.of(page, size, sortByNombre);
    
                Page<Cliente> clientesPaginados = clienteService.findAll(pageable);
    
                clientes = clientesPaginados.getContent();
    
                responseEntity = new ResponseEntity<List<Cliente>>(clientes, HttpStatus.OK);
            
            } catch (Exception e) {
                responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
             }



            } else {
                try {
                    clientes = clienteService.findAll(sortByNombre);
                    responseEntity = new ResponseEntity<List<Cliente>>(clientes, HttpStatus.OK);
                } catch (Exception e) {
                    responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }


            }

    return responseEntity;
    } 

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable(name = "id") Long id) {

        ResponseEntity<Map<String, Object>> responseEntity = null;

        Map<String, Object> responseAsMap = new HashMap<>();
        try {
            Cliente cliente = clienteService.findById(id);
            if (cliente != null) {
                String succesMessage = "Se ha encontrado el cliente con id: " + id;
                responseAsMap.put("mensaje", succesMessage);
                responseAsMap.put("cliente", cliente);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);
            } else {
                String errorMessage = "No se ha encontrado el cliente con id: " + id;
                responseAsMap.put("error", errorMessage);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            String errorGrave = "Error grave";
            responseAsMap.put("error", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);

        }
        return responseEntity;

    }

    // @throws IOException
    @PostMapping(consumes = "multipart/form-data") 
    @Transactional
    public ResponseEntity<Map<String, Object>> insert(@Valid @RequestPart(name = "cliente") Cliente cliente,
                                                                BindingResult result,
                                                                @RequestPart(name = "file") MultipartFile file) throws IOException {
                                                   

        Map<String, Object> responseAsMap = new HashMap<>();

        ResponseEntity<Map<String, Object>> responseEntity = null;
        /** Primero comprobar si hay errores en el cliente recibido */
        if (result.hasErrors()) {
            List<String> errorMessages = new ArrayList<>();
            for (ObjectError error : result.getAllErrors()) {

                errorMessages.add(error.getDefaultMessage());

            }
            responseAsMap.put("errores", errorMessages);

            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.BAD_REQUEST);

            return responseEntity; // si hay error no quiero que se guarde el cliente
        }

        // Si no hay errores, entonces persistimos el cliente.
        // COmprobando previamente si nos han enviado una imagen o un archivo
        
        if(!file.isEmpty()){
            String fileCode = fileUploadUtil.saveFile(file.getOriginalFilename(), file);
            cliente.setImagenCliente(fileCode + "-" + file.getOriginalFilename());
       // Devolver informacion respecto al file recibido

       FileUploadResponse fileUploadResponse = FileUploadResponse.builder()
       .fileName(fileCode + "-" + file.getOriginalFilename())
       .downloadURI("/clientes/downloadFile/" + fileCode + "-" 
               + file.getOriginalFilename())
       .size(file.getSize())
       .build();

        responseAsMap.put("info de la imagen: ", fileUploadResponse);
    }
        Cliente clienteDB = clienteService.save(cliente);
        try {
            if (clienteDB != null) {
                List<Mascota> mascotas =cliente.getMascotas();
                    if(mascotas != null){
                        for(Mascota mascota: mascotas){
                            mascota.setCliente(clienteDB);
                            mascotaService.save(mascota);
                        }
                    }
                // Aqui estoy haciendo la validacion de si se ha guardado
                String mensaje = "El cliente se ha creado correctamente";
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("cliente", clienteDB);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.CREATED);
                
            } else {
                String mensaje = "El cliente no se ha creado";
                responseAsMap.put("mensaje", mensaje);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.NOT_ACCEPTABLE);
            }
        } catch (DataAccessException e) {
            // Tipo de error de DAtaAccesException tipo de error controlado
            String errorGrave = "Ha tenido lugar un error grave y la causa más probable puede ser" +
                    e.getMostSpecificCause(); // Lo que te devuelve aqui es lo mas cercano al error mas probable (ej
                                              // caused by)
            responseAsMap.put("errorGrave", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Cliente cliente, BindingResult result,
            @PathVariable(name = "id") Integer id) {
        // Para que valide lo que llega

        Map<String, Object> responseAsMap = new HashMap<>();

        ResponseEntity<Map<String, Object>> responseEntity = null;
        /** Primero comprobar si hay errores en el cliente recibido */
        if (result.hasErrors()) {
            List<String> errorMessages = new ArrayList<>();
            for (ObjectError error : result.getAllErrors()) {

                errorMessages.add(error.getDefaultMessage());

            }
            responseAsMap.put("errores", errorMessages);

            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.BAD_REQUEST);

            return responseEntity; // si hay error no quiero que se guarde el cliente
        }

        // Vinculamos el id que se recibe con el cliente
        cliente.setId(id);
        // Si no hay errores, entonces actualizamos el cliente.
        Cliente clienteDB = clienteService.save(cliente);
        try {
            if (clienteDB != null) { // Aqui estoy haciendo la validacion de si se ha guardado
                String mensaje = "El cliente se ha actualizado correctamente";
                responseAsMap.put("mensaje", mensaje);
                responseAsMap.put("cliente", clienteDB);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.OK);

            } else {
                String mensaje = "El cliente no se ha actualizado";
                responseAsMap.put("mensaje", mensaje);
                responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.NOT_ACCEPTABLE);
            }
        } catch (DataAccessException e) {
            // Tipo de error de DAtaAccesException tipo de error controlado
            String errorGrave = "Ha tenido lugar un error grave y la causa más probable puede ser" +
                    e.getMostSpecificCause(); // Lo que te devuelve aqui es lo mas cercano al error mas probable (ej
                                              // caused by)
            responseAsMap.put("errorGrave", errorGrave);
            responseEntity = new ResponseEntity<Map<String, Object>>(responseAsMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseEntity;
    }
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminacliente(@PathVariable(name = "id") Integer id){
        ResponseEntity<String> responseEntity = null;
       
        Cliente cliente = clienteService.findById(id);
      
        try {
            if (cliente != null) {
            String mensaje = "El cliente se ha borrado correctamente";
            clienteService.delete(cliente);
            responseEntity = new ResponseEntity<String>(mensaje, HttpStatus.OK);
        } else{
            responseEntity = new ResponseEntity<String>("No existe el cliente",HttpStatus.NO_CONTENT);
        }
    } catch (DataAccessException e) {
           e.getMostSpecificCause();
            String errorGrave = "Error grave";
            responseEntity = new ResponseEntity<String>(errorGrave, HttpStatus.INTERNAL_SERVER_ERROR);
            
        }
        return responseEntity;
    }

    @GetMapping("/downloadFile/{fileCode}")
    public ResponseEntity<?> downloadFile(@PathVariable(name = "fileCode") String fileCode) { // Devuelve un generico de cualquier cosa

        Resource resource = null; // EL objetivo es que me devuelva un recurso

        try {
            resource = fileDownloadUtil.getFileAsResource(fileCode);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build(); // Otra manera de devolver responseEntity
        }

        if (resource == null) {
            return new ResponseEntity<>("File not found ", HttpStatus.NOT_FOUND);
        }

        String contentType = "application/octet-stream";
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType)) //Hay que especificarle el tipo contenttype, viene de arriva
        .header(HttpHeaders.CONTENT_DISPOSITION, headerValue) // En la cabecera te digo que te mando un archivo como atachment
        .body(resource);

        // TOdo lo que es imagen o hipertexto va con el get
    }
    
}
