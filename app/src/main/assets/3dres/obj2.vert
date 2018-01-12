attribute vec3 vPosition;
attribute vec2 vCoord;
uniform mat4 vMatrix;
uniform vec3 vKa;
uniform vec3 vKd;
uniform vec3 vKs;

varying vec2 textureCoordinate;

attribute vec3 vNormal;         //Normal vector
varying vec4 vDiffuse;          //The final intensity of the scattered light that is passed to the fragment shader
varying vec4 vAmbient;          //The final intensity of the ambient light passed to the fragment shader
varying vec4 vSpecular;          //The final intensity of the specular light passed to the fragment shader

//Positioning light light calculation method
void calculateLight(             //Positioning light light calculation method
  vec3 normal,               //Normal vector
  vec4 vA,
  vec4 vD,
  vec4 vS,
  vec3 camera,
  vec3 lightLocation,        //Light position
  vec4 lightAmbient,         //Ambient light intensity
  vec4 lightDiffuse,         //Scattered light intensity
  vec4 lightSpecular         //Mirror light intensity
){
  vA=lightAmbient;         //Directly draw the final intensity of ambient light
  vec3 normalTarget=vPosition+normal;   //The transformed normal vector is calculated
  vec3 newNormal=(vMatrix*vec4(normalTarget,1)).xyz-(vMatrix*vec4(vPosition,1)).xyz;
  newNormal=normalize(newNormal);   //Normalize the normal vector
  //Calculate the vector from the surface point to the camera
  vec3 eye= normalize(camera-(vMatrix*vec4(vPosition,1)).xyz);
  //Calculate the vector vp from the surface point to the light source position
  vec3 vp= normalize(lightLocation-(vMatrix*vec4(vPosition,1)).xyz);
  vp=normalize(vp);//Format vp
  vec3 halfVector=normalize(vp+eye);    //Seeking sight and light half-vector
  float shininess=50.0;             //Roughness, smaller and smoother
  float nDotViewPosition=max(0.0,dot(newNormal,vp));    //The sum of the dot product of the summing vector and vp with 0
  vD=lightDiffuse*nDotViewPosition;                //Calculate the final intensity of the scattered light
  float nDotViewHalfVector=dot(newNormal,halfVector);   //The dot product of normal and semi-vector
  float powerFactor=max(0.0,pow(nDotViewHalfVector,shininess));     //Specular reflected light intensity factor
  vS=lightSpecular*powerFactor;               //Calculate the final intensity of the specular light
}

void main(){
    gl_Position = vMatrix*vec4(vPosition,1);
    textureCoordinate = vCoord;

    vec3 lightLocation=vec3(0.0,-200.0,-500.0);      //Light position
    vec3 camera=vec3(0,200.0,0);
    float shininess=10.0;             //Roughness, smaller and smoother
//    vec4 a,d,s;
//    calculateLight(normalize(vNormal),a,d,s,camera,pos,vec4(vKa,1.0),vec4(vKd,1.0),vec4(vKs,1.0));
//    vAmbient=a;
//    vDiffuse=d;
//    vSpecular=s;


     vec3 newNormal=normalize((vMatrix*vec4(vNormal+vPosition,1)).xyz-(vMatrix*vec4(vPosition,1)).xyz);
     vec3 vp=normalize(lightLocation-(vMatrix*vec4(vPosition,1)).xyz);
     vDiffuse=vec4(vKd,1.0)*max(0.0,dot(newNormal,vp));                //Calculate the final intensity of the scattered light

     vec3 eye= normalize(camera-(vMatrix*vec4(vPosition,1)).xyz);
     vec3 halfVector=normalize(vp+eye);    //Seeking sight and light half-vector
     float nDotViewHalfVector=dot(newNormal,halfVector);   //The dot product of normal and semi-vector
     float powerFactor=max(0.0,pow(nDotViewHalfVector,shininess));     //Specular reflected light intensity factor
     vSpecular=vec4(vKs,1.0)*powerFactor;               //Calculate the final intensity of the specular light

     vAmbient=vec4(vKa,1.0);
}