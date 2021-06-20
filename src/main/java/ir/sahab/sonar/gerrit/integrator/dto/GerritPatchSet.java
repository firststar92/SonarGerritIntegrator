package ir.sahab.sonar.gerrit.integrator.dto;

public class GerritPatchSet {
    public final int changeId;
    public final int patchSetNumber;

    public GerritPatchSet(int changeId, int patchSetNumber) {
        this.changeId = changeId;
        this.patchSetNumber = patchSetNumber;
    }
}
