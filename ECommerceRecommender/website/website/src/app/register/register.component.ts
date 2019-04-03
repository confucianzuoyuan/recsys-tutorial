import { Component, OnInit } from '@angular/core';
import {LoginService} from "../services/login.service";
import {User} from "../model/user";

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {

  loginService: LoginService;
  user: User = new User;

  constructor(loginService:LoginService) {
    this.loginService = loginService;
  }

  ngOnInit() {}

  register():void{
    this.loginService.register(this.user);
  }

}
