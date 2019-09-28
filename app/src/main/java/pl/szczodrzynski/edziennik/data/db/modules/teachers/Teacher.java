package pl.szczodrzynski.edziennik.data.db.modules.teachers;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.utils.Utils.bs;
import static pl.szczodrzynski.edziennik.utils.Utils.ns;

@Entity(tableName = "teachers",
        primaryKeys = {"profileId", "teacherId"})
public class Teacher {
    public int profileId;

    @ColumnInfo(name = "teacherId")
    public long id;

    @ColumnInfo(name = "teacherLoginId")
    public String loginId = null;

    @ColumnInfo(name = "teacherName")
    public String name;
    @ColumnInfo(name = "teacherSurname")
    public String surname;

    @ColumnInfo(name = "teacherType")
    public int type;
    public static final int TYPE_TEACHER = 0; // 1
    public static final int TYPE_EDUCATOR = 1; // 2
    public static final int TYPE_PEDAGOGUE = 2; // 4
    public static final int TYPE_LIBRARIAN = 3; // 8
    public static final int TYPE_SECRETARIAT = 4; // 16
    public static final int TYPE_PRINCIPAL = 5; // 32
    public static final int TYPE_SCHOOL_ADMIN = 6; // 64
    // not teachers
    public static final int TYPE_SUPER_ADMIN = 7; // 128
    public static final int TYPE_STUDENT = 8; // 256
    public static final int TYPE_PARENT = 9; // 512
    public static final int TYPE_PARENTS_COUNCIL = 10; // 1024
    public static final int TYPE_SCHOOL_PARENTS_COUNCIL = 11; // 2048
    public static final int TYPE_OTHER = 12; // 4096
    public static final int IS_TEACHER_MASK = 0b1111111;
    @ColumnInfo(name = "teacherTypeDescription")
    public String typeDescription = null;

    public boolean isType(int checkingType) {
        return (type & (1 << checkingType)) >= 1;
    }
    public boolean isTeacher() {
        return type <= IS_TEACHER_MASK;
    }
    public void setType(int setType) {
        type |= (1 << setType);
    }
    public void unsetType(int unsetType) {
        type &= ~(1 << unsetType);
    }

    public static String typeString(Context c, int type) {
        return typeString(c, type, null);
    }
    public static String typeString(Context c, int type, String typeDescription) {
        String suffix = bs(" - ", typeDescription);
        switch (type) {
            default:
            case TYPE_TEACHER:
                return c.getString(R.string.teacher_teacher)+suffix;
            case TYPE_PARENTS_COUNCIL:
                return c.getString(R.string.teacher_parents_council)+suffix;
            case TYPE_SCHOOL_PARENTS_COUNCIL:
                return c.getString(R.string.teacher_school_parents_council)+suffix;
            case TYPE_PEDAGOGUE:
                return c.getString(R.string.teacher_pedagogue)+suffix;
            case TYPE_LIBRARIAN:
                return c.getString(R.string.teacher_librarian)+suffix;
            case TYPE_SCHOOL_ADMIN:
                return c.getString(R.string.teacher_school_admin)+suffix;
            case TYPE_SUPER_ADMIN:
                return c.getString(R.string.teacher_super_admin)+suffix;
            case TYPE_SECRETARIAT:
                return c.getString(R.string.teacher_secretariat)+suffix;
            case TYPE_PRINCIPAL:
                return c.getString(R.string.teacher_principal)+suffix;
            case TYPE_EDUCATOR:
                return c.getString(R.string.teacher_educator)+suffix;
            case TYPE_PARENT:
                return c.getString(R.string.teacher_parent)+suffix;
            case TYPE_STUDENT:
                return c.getString(R.string.teacher_student)+suffix;
            case TYPE_OTHER:
                return c.getString(R.string.teacher_other)+suffix;
        }
    }
    public String getType(Context c) {
        return typeString(c, Utils.leftmostSetBit(type), typeDescription);
    }

    @Ignore
    public Bitmap image = null;
    @Ignore
    public String displayName = null;

    @Ignore
    public Teacher(int profileId, long id, String name, String surname) {
        this.profileId = profileId;
        this.id = id;
        this.name = name;
        this.surname = surname;
    }

    public Teacher(int profileId, long id, String name, String surname, String loginId) {
        this.profileId = profileId;
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.loginId = loginId;
    }

    public String getFullName()
    {
        return name+" "+surname;
    }
    public String getFullNameLastFirst()
    {
        return surname+" "+name;
    }

    public String getShortName()
    {
        return (name.length() == 0 ? " " : name.charAt(0))+"."+surname;
    }

    // USED IN IUCZNIOWIE + getRecipientList
    public static Teacher getById(List<Teacher> teacherList, long id)
    {
        for (Teacher teacher: teacherList) {
            if (teacher.id == id)
            {
                return teacher;
            }
        }
        return null;
    }
    public static Teacher getByFullName(List<Teacher> teacherList, String name)
    {
        for (Teacher teacher: teacherList) {
            if ((teacher.name + " " + teacher.surname).equals(name))
            {
                return teacher;
            }
        }
        return null;
    }
    public static Teacher getByFullNameLastFirst(List<Teacher> teacherList, String name)
    {
        for (Teacher teacher: teacherList) {
            if ((teacher.surname + " " + teacher.name).equals(name))
            {
                return teacher;
            }
        }
        return null;
    }
    public static Teacher getByShortName(List<Teacher> teacherList, String shortName) /* F.Lastname */
    {
        for (Teacher teacher: teacherList) {
            if (teacher.name.length() > 0 && ((teacher.name.charAt(0) + "." + teacher.surname).equals(shortName)))
            {
                return teacher;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return getFullName();
    }
}
