package com.example.ptut.mm_kunyi.models

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.ptut.mm_kunyi.models.base.BaseModel
import com.example.ptut.mm_kunyi.utils.AppConstants
import com.example.ptut.mm_kunyi.utils.EmptyError
import com.example.ptut.mm_kunyi.utils.Error
import com.example.ptut.mm_kunyi.vos.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import org.w3c.dom.Comment
import java.util.*
import kotlin.collections.ArrayList

class JobListModel private constructor(context: Context) : BaseModel(context) {
    companion object {
        private lateinit var mDatabaseReference: DatabaseReference
        private lateinit var mFirebaseAuth: FirebaseAuth
        private var mFirebaseUser: FirebaseUser? = null
        private lateinit var mJobInfoDR:DatabaseReference
        private var INSTANCE: JobListModel? = null
        fun getInstance(): JobListModel {
            if (INSTANCE == null) {
                throw RuntimeException("HealthCareInfoModel is being invoked before initializing.")
            }
            val i = INSTANCE
            return i!!
        }

        fun initJobListModel(context: Context) {
            INSTANCE = JobListModel(context)
        }
    }
    fun getJobListData(mErrorLD: MutableLiveData<Error>) {
        mJobInfoDR.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val jobListArray = ArrayList<JobListVO>()
                if (dataSnapshot != null) {
                    for (snapshot in dataSnapshot.children) {
                        val jobListItem = snapshot.getValue<JobListVO>(JobListVO::class.java)
                        jobListItem!!.jobId=snapshot.ref.key
                        if(jobListItem.like==null){
                            jobListItem.like=ArrayList()
                        }
                        if(jobListItem.comment==null){
                          jobListItem.comment=ArrayList()
                        }
                        Log.e("key",jobListItem.jobId)
                        jobListArray.add(jobListItem)
                    }
                    savePersistence(jobListArray)
                } else {
                    mErrorLD.value = EmptyError("Null Data")
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                mErrorLD.value = com.example.ptut.mm_kunyi.utils.DatabaseError(databaseError.message)
            }
        })
    }
    private fun savePersistence(jobList: List<JobListVO>) {
        val ids: LongArray = mTheDB.jobListDao().insertAll(jobList)
        Log.e("JobList", "$ids")
    }

    fun getJobList(): LiveData<List<JobListVO>> { return mTheDB.jobListDao().getJobList() }
    fun getJobById(jobId: Int): LiveData<JobListVO> { return mTheDB.jobListDao().getJobById(jobId) }
    fun getJobByIdComment(jobId: String): LiveData<JobListVO> {
        Log.e("List",mTheDB.jobListDao().getJobByIdComment(jobId).value.toString())
        return mTheDB.jobListDao().getJobByIdComment(jobId)
    }

    fun authenticateUserWithGoogleAccount(signInAccount: GoogleSignInAccount, delegate: SignInWithGoogleAccountDelegate) {
        val credential = GoogleAuthProvider.getCredential(signInAccount.idToken, null)
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        delegate.onFailureSignIn(task.exception!!.message!!)
                    } else {
                        delegate.onSuccessSignIn(signInAccount)
                    }
                }
                .addOnFailureListener { e ->
                    delegate.onFailureSignIn(e.message!!)
                }
    }

    fun isUserSignIn(): Boolean { return mFirebaseUser != null }
    fun getAuth(): FirebaseAuth { return mFirebaseAuth }
    fun getUserInfo(): FirebaseUser { return mFirebaseUser!! }

    fun applyJob(jobId:String,applicantId:String,callback: ApplyCallBack){
        var applicant:ApplicantVO= ApplicantVO.initApplicant(mFirebaseUser!!.displayName,
                mFirebaseUser!!.photoUrl.toString())
        mJobInfoDR.child(jobId).child("applicant")
                .child(applicantId).setValue(applicant)
        callback.onApplySuccess("Success Apply Your Information!!")
    }

    fun addLike(jobId:String,likeId:String){
        if(mFirebaseUser!=null){
            var like:LikeVO= LikeVO.initLike(mFirebaseUser!!.uid)
            mJobInfoDR.child(jobId).child("like")
                    .child(likeId).setValue(like)
        }

    }
    fun addUnLike(jobId:String,likeId:String){
        var like:LikeVO= LikeVO.initLike(mFirebaseUser!!.uid)
        mJobInfoDR.child(jobId).child("like")
                .child(likeId).removeValue()
    }

    fun addComment(jobId: String,commentId:String,details:String){
        var comment=CommentVO.initComment(mFirebaseUser!!.uid, mFirebaseUser!!.displayName!!,
                mFirebaseUser!!.photoUrl!!.toString(),details)
        mJobInfoDR.child(jobId).child("comment")
                .child(commentId).setValue(comment)
    }

    init {
        mDatabaseReference = FirebaseDatabase.getInstance().reference
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth.currentUser
        mJobInfoDR = mDatabaseReference.child(AppConstants.KUNYI_INFO_DR)
    }

    interface ApplyCallBack{
        fun onApplySuccess(msg:String)
    }
    interface SignInWithGoogleAccountDelegate {
        fun onSuccessSignIn(signInAccount: GoogleSignInAccount)
        fun onFailureSignIn(msg: String)
    }


}