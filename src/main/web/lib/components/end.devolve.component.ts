import {Component, OnInit} from '@angular/core';
import {PharmacyService} from '../services/pharmacy.service';
import {DevolveService} from '../services/devolve.service';
import {NotificationService} from '@alfresco/adf-core';
import {AppLoaderService} from '@lamis/web-core';
import {ActivatedRoute} from '@angular/router';
import {Devolve} from '../model/pharmacy.model';
import {Observable} from 'rxjs';
import {HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {Moment} from 'moment';
import * as moment_ from 'moment';

const moment = moment_;

@Component({
    selector: 'end-devolve',
    templateUrl: './end.devolve.component.html'
})
export class EndDevolveComponent implements OnInit {
    entity: Devolve = {};
    dmocType: string = '';
    isSaving = false;
    minDate: Moment;
    minDiscontinued: Moment;
    today = moment();

    constructor(private pharmacyService: PharmacyService,
                private devolveService: DevolveService,
                protected notification: NotificationService,
                private appLoaderService: AppLoaderService,
                protected activatedRoute: ActivatedRoute) {
    }

    ngOnInit(): void {
        const patientId = this.activatedRoute.snapshot.paramMap.get('patientId');
        this.pharmacyService.getPatient(patientId).subscribe((res) => {
            this.pharmacyService.getDevolvement(res.id, moment()).subscribe(r => {
                this.entity = r;
                if (this.entity.dateDiscontinued) {
                    this.minDate = r.dateDiscontinued.clone().add(2, 'day');
                } else {
                    this.minDiscontinued = r.dateDevolved.clone().add(1, 'day');
                    this.minDate = r.dateDevolved.clone().add(2, 'day');
                }

                let type = 'MMD';
                switch (r.dmocType) {
                    case 'ARC':
                        type = 'Adolescent Refill Club';
                        break;
                    case 'CPARP':
                        type = 'CPARP';
                        break;
                    case 'CARC':
                        type = 'CARC';
                        break;
                    case 'F_CARG':
                        type = 'F-CARG';
                        break;
                    case 'FAST_TRACK':
                        type = 'Fast Track';
                        break;
                    case 'S_CARG':
                        type = 'S-CARG';
                        break;
                    case 'MMS':
                        type = 'MMS';
                        break;
                }
                this.dmocType = type;
            });
        });
        this.activatedRoute.data.subscribe(({entity}) => {
            this.entity = !!entity && entity.body ? entity.body : entity;
        });

    }

    dateDiscontinuedChanged() {
        if (this.entity.dateDiscontinued) {
            this.minDate = this.entity.dateDiscontinued.clone().add(1, 'day');
        }
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.appLoaderService.open('Saving visit...');
        this.isSaving = true;
        if (this.entity.id !== undefined) {
            this.subscribeToSaveResponse(this.devolveService.update(this.entity));
        } else {
            this.subscribeToSaveResponse(this.devolveService.create(this.entity));
        }
    }

    private subscribeToSaveResponse(result: Observable<HttpResponse<any>>) {
        result.subscribe(
            (res: HttpResponse<any>) => this.onSaveSuccess(res.body),
            (res: HttpErrorResponse) => {
                this.appLoaderService.close();
                this.onSaveError();
                this.onError(res.message);
            });
    }

    private onSaveSuccess(result: any) {
        this.appLoaderService.close();
        this.isSaving = false;
        this.notification.showInfo('Devolve successfully saved');
        this.previousState();
    }

    private onSaveError() {
        this.isSaving = false;
        this.notification.showError('Error saving devolve');
    }

    protected onError(errorMessage: string) {
        this.isSaving = false;
        this.notification.showError(errorMessage);
    }
}
