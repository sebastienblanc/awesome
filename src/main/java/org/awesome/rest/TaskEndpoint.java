package org.awesome.rest;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.awesome.model.Task;
import org.jboss.aerogear.unifiedpush.JavaSender;
import org.jboss.aerogear.unifiedpush.SenderClient;
import org.jboss.aerogear.unifiedpush.message.UnifiedMessage;

/**
 * 
 */
@Stateless
@Path("/tasks")
public class TaskEndpoint
{
   @PersistenceContext(unitName = "awesome-persistence-unit")
   private EntityManager em;

   @POST
   @Consumes("application/json")
   public Response create(Task entity)
   {
      em.persist(entity);
      JavaSender defaultJavaSender = new SenderClient.Builder("https://javaoneups-sblanc.rhcloud.com/ag-push/").build();
     
      UnifiedMessage unifiedMessage = new UnifiedMessage.Builder()
              .pushApplicationId("586ececd-bce1-48e1-9e49-080127a02150")
              .masterSecret("7fa8d801-fdd0-40a8-843e-48c1216f08fe")
              .alert("Task : " + entity.getName() + " has been created")
              .build();
      defaultJavaSender.send(unifiedMessage);
      
      return Response.created(UriBuilder.fromResource(TaskEndpoint.class).path(String.valueOf(entity.getId())).build()).build();
   }

   @DELETE
   @Path("/{id:[0-9][0-9]*}")
   public Response deleteById(@PathParam("id") Long id)
   {
      Task entity = em.find(Task.class, id);
      if (entity == null)
      {
         return Response.status(Status.NOT_FOUND).build();
      }
      em.remove(entity);
      return Response.noContent().build();
   }

   @GET
   @Path("/{id:[0-9][0-9]*}")
   @Produces("application/json")
   public Response findById(@PathParam("id") Long id)
   {
      TypedQuery<Task> findByIdQuery = em.createQuery("SELECT DISTINCT t FROM Task t WHERE t.id = :entityId ORDER BY t.id", Task.class);
      findByIdQuery.setParameter("entityId", id);
      Task entity;
      try
      {
         entity = findByIdQuery.getSingleResult();
      }
      catch (NoResultException nre)
      {
         entity = null;
      }
      if (entity == null)
      {
         return Response.status(Status.NOT_FOUND).build();
      }
      return Response.ok(entity).build();
   }

   @GET
   @Produces("application/json")
   public List<Task> listAll(@QueryParam("start") Integer startPosition, @QueryParam("max") Integer maxResult)
   {
      TypedQuery<Task> findAllQuery = em.createQuery("SELECT DISTINCT t FROM Task t ORDER BY t.id", Task.class);
      if (startPosition != null)
      {
         findAllQuery.setFirstResult(startPosition);
      }
      if (maxResult != null)
      {
         findAllQuery.setMaxResults(maxResult);
      }
      final List<Task> results = findAllQuery.getResultList();
      return results;
   }

   @PUT
   @Path("/{id:[0-9][0-9]*}")
   @Consumes("application/json")
   public Response update(Task entity)
   {
      try
      {
         entity = em.merge(entity);
      }
      catch (OptimisticLockException e)
      {
         return Response.status(Response.Status.CONFLICT).entity(e.getEntity()).build();
      }

      return Response.noContent().build();
   }
}