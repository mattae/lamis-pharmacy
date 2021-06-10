import {NgModule} from '@angular/core';
import {TptCompletionComponent} from './components/tpt-completion.component';
import {MatButtonModule, MatCardModule, MatDatepickerModule, MatInputModule} from '@angular/material';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {MatDateFormatModule} from '@lamis/web-core';
import {RouterModule, Routes} from '@angular/router';

const ROUTES: Routes = [
    {
        path: '',
        data: {
            title: 'TPT Completion',
            breadcrumb: 'TPT COMPLETION'
        },
        children: [
            {
                path: 'patient/:patientId/new',
                component: TptCompletionComponent,
                data: {
                    authorities: ['ROLE_DEC'],
                    title: 'TPT Completion',
                    breadcrumb: 'TPT COMPLETION'
                },
                //canActivate: [UserRouteAccessService]
            }
        ]
    }
];


@NgModule({
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatInputModule,
        MatDatepickerModule,
        FormsModule,
        MatDateFormatModule,
        RouterModule.forChild(ROUTES)
    ],
    declarations: [
        TptCompletionComponent
    ]
})
export class TptModule {

}
