uniform mat4 vMatrix;        //Total transformation matrix
uniform mat4 uMMatrix;          //Transform matrix
uniform vec3 uLightLocation;        //Light position
uniform vec3 uCamera;           //Camera location
attribute vec3 vPosition;       //Vertex position
attribute vec3 vNormal;         //Normal vector
varying vec4 vDiffuse;          //The final intensity of the scattered light that is passed to the fragment shader


//Backscattered light intensity
vec4 pointLight(vec3 normal,vec3 lightLocation,vec4 lightDiffuse){
    //The transformed normal vector
    vec3 newTarget=normalize((vMatrix*vec4(normal+vPosition,1)).xyz-(vMatrix*vec4(vPosition,1)).xyz);
    //Surface point and light source of the direction vector
    vec3 vp=normalize(lightLocation-(vMatrix*vec4(vPosition,1)).xyz);
    return lightDiffuse*max(0.0,dot(newTarget,vp));
}

void main(){
   gl_Position = vMatrix * vec4(vPosition,1); //According to the total transformation matrix calculation this vertex position

   vec4 at=vec4(1.0,1.0,1.0,1.0);   //Light intensity
   vec3 pos=vec3(80.0,80.0,80.0);      //Light position
   vDiffuse=pointLight(normalize(vPosition),pos,at);
}