import {Injectable} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot, Routes} from '@angular/router';
import {Observable, of} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {Devolve} from '../model/pharmacy.model';
import {DevolveEditComponent} from '../components/devolve.edit.component';
import {DevolveDetailsComponent} from '../components/devolve.details.component';
import {DevolveService} from './devolve.service';
import {EndDevolveComponent} from '../components/end.devolve.component';

@Injectable()
export class DevolveResolve implements Resolve<Devolve> {
    constructor(private service: DevolveService) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Devolve> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.findByUuid(id).pipe(
                filter((response: HttpResponse<Devolve>) => response.ok),
                map((patient: HttpResponse<Devolve>) => patient.body)
            );
        }
        return of(<Devolve>{});
    }
}

export const ROUTES: Routes = [
    {
        path: '',
        data: {
            title: 'Client Devolvement',
            breadcrumb: 'CLIENT DEVOLVEMENT'
        },
        children: [
            {
                path: ':id/patient/:patientId/view',
                component: DevolveDetailsComponent,
                resolve: {
                    entity: DevolveResolve
                },
                data: {
                    authorities: ['ROLE_USER'],
                    title: 'Client Devolve',
                    breadcrumb: 'CLIENT DEVOLVE'
                },
                //canActivate: [UserRouteAccessService]
            },
            {
                path: 'patient/:patientId/new',
                component: DevolveEditComponent,
                data: {
                    authorities: ['ROLE_DEC'],
                    title: 'Client Devolve',
                    breadcrumb: 'DEVOLVE CLIENT'
                },
                //canActivate: [UserRouteAccessService]
            },
            {
                path: ':id/patient/:patientId/edit',
                component: DevolveEditComponent,
                resolve: {
                    entity: DevolveResolve
                },
                data: {
                    authorities: ['ROLE_DEC'],
                    title: 'Devolve Edit',
                    breadcrumb: 'DEVOLVE EDIT'
                },
                //canActivate: [UserRouteAccessService]
            },
            {
                path: 'return/patient/:patientId/new',
                component: EndDevolveComponent,
                data: {
                    authorities: ['ROLE_DEC'],
                    title: 'End Devolve',
                    breadcrumb: 'END CLIENT DEVOLVE'
                },
                //canActivate: [UserRouteAccessService]
            }
        ]
    }
];

