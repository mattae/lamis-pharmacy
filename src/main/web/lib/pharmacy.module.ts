import {CoreModule} from '@alfresco/adf-core';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule
} from '@angular/material';
import {RouterModule} from '@angular/router';
import {CovalentDialogsModule, CovalentMessageModule} from '@covalent/core';
import {PharmacyDetailsComponent} from './components/pharmacy-details.component';
import {PharmacyEditComponent} from './components/pharmacy-edit.component';
import {PharmacyResolve, ROUTES} from './services/pharmacy.route';
import {PharmacyWidgetModule} from './pharmacy.widget.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NgxDatatableModule} from '@swimlane/ngx-datatable';
import {MatDateFormatModule} from '@lamis/web-core';
import {CustomFormsModule} from 'ng2-validation';

@NgModule({
    declarations: [
        PharmacyDetailsComponent,
        PharmacyEditComponent
    ],
    imports: [
        CommonModule,
        MatInputModule,
        MatIconModule,
        MatCardModule,
        MatSelectModule,
        MatButtonModule,
        RouterModule.forChild(ROUTES),
        MatProgressBarModule,
        FormsModule,
        CovalentMessageModule,
        CovalentDialogsModule,
        MatTableModule,
        MatListModule,
        CoreModule,
        PharmacyWidgetModule,
        NgxDatatableModule,
        ReactiveFormsModule,
        MatDateFormatModule,
        CustomFormsModule
    ],
    exports: [
        PharmacyDetailsComponent,
        PharmacyEditComponent
    ],
    entryComponents: [],
    providers: [
        PharmacyResolve
    ]
})
export class PharmacyModule {
}
