import {Component, OnInit} from '@angular/core';
import {PharmacyService} from '../services/pharmacy.service';
import {ActivatedRoute} from '@angular/router';
import {Patient} from '../model/pharmacy.model';
import * as moment_ from 'moment';
import {Moment} from 'moment';
import {NotificationService} from '@alfresco/adf-core';

const moment = moment_;

@Component({
    selector: 'tpt-completion',
    templateUrl: './tpt-completion.component.html'
})
export class TptCompletionComponent implements OnInit {
    patient: Patient;
    lastIptDate: Moment;
    minIptCompletion: Moment;
    dateOfCompletion: Moment;
    today = moment();
    isSaving = false;

    constructor(private pharmacyService: PharmacyService, private activatedRoute: ActivatedRoute,
                private notification: NotificationService) {
    }

    ngOnInit(): void {
        const patientId = this.activatedRoute.snapshot.paramMap.get('patientId');
        this.pharmacyService.getPatient(patientId).subscribe((res) => {
            this.patient = res;
            this.pharmacyService.dateOfLastIptBefore(this.patient.id, moment()).subscribe(res1 => {
                this.lastIptDate = res1;
                this.minIptCompletion = this.lastIptDate.clone().add(6, 'months').subtract(13, 'days');
            });
        });
    }

    save() {
        this.isSaving = true;
        this.pharmacyService.saveTptCompletion(this.patient.id, this.dateOfCompletion).subscribe(
            (res) => {
                this.notification.showInfo('TPT Completion successfully saved');
                this.previousState();
            },
            (error) => {
                this.notification.showError('Error saving TPT Completions');
                this.isSaving = false;
            }
        );
    }

    previousState() {
        window.history.back();
    }
}
