package com.library.domain.users;

import com.library.domain.DirectParent;
import com.library.domain.books.BooksController;
import com.library.domain.books.reservations.ReservationsService;
import com.library.domain.events.EventsController;
import com.library.domain.users.permissions.PermissionsService;
import com.library.infrastructure.InfrastructureParent;
import com.library.infrastructure.email.TheLino;

public class UsersController {

    public UsersController() {

    }

    public void usersController(BooksController bk) {
        //BooksController booksController = new BooksController();
        //ReservationsService reservationsService = new ReservationsService();
        //EventsController eventsController = new EventsController();
        //TestUserino testUserino = new TestUserino();
        //DirectParent directParent = new DirectParent();
        //PermissionsService permissionsService = new PermissionsService();
        InfrastructureParent infrastructureParent = new InfrastructureParent();
        //TheLino theLino = new TheLino();
    }

}
